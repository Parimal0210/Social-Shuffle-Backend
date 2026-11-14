package com.socialshuffle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialshuffle.model.*;
import com.socialshuffle.repository.RegistrationRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final AppConfigService appConfigService;
    private final RegistrationRepository registrationRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile String lastSenderFingerprint = "";
    private volatile JavaMailSenderImpl cachedMailSender;

    public record EmailDeliveryResult(boolean success, String provider, String messageId, String error) {}

    public EmailNotificationService(AppConfigService appConfigService,
                                    RegistrationRepository registrationRepository,
                                    ObjectMapper objectMapper) {
        this.appConfigService = appConfigService;
        this.registrationRepository = registrationRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    /**
     * Core real multi-provider email transport:
     * 1) Gmail OAuth2 REST API (if connected via OAuth)
     * 2) SMTP via JavaMailSenderImpl (primary port + 465 SSL failover)
     * 3) Resend HTTPS REST API (if RESEND_API_KEY configured)
     */
    public EmailDeliveryResult dispatchEmail(String to, String subject, String html, String plainText) {
        if (to == null || !to.contains("@")) {
            return new EmailDeliveryResult(false, "none", null, "Invalid recipient email address");
        }
        String cleanTo = to.trim();
        Map<String, Object> cfg = appConfigService.getCommunicationsConfig();
        String fromAddr = resolveFromAddress(cfg);
        List<String> errors = new ArrayList<>();

        // 1. Try Gmail OAuth2 REST API if connected & token present
        String gmailAccessToken = str(cfg.get("gmailAccessToken"));
        boolean gmailConnected = Boolean.TRUE.equals(cfg.get("gmailConnected")) && !gmailAccessToken.isEmpty();
        long gmailExpiresAt = cfg.get("gmailExpiresAt") instanceof Number ? ((Number) cfg.get("gmailExpiresAt")).longValue() : 0L;
        if (gmailConnected && (gmailExpiresAt == 0L || gmailExpiresAt > System.currentTimeMillis())) {
            EmailDeliveryResult gmailRes = sendViaGmailApi(gmailAccessToken, str(cfg.get("gmailEmail")), cleanTo, subject, html);
            if (gmailRes.success()) {
                return gmailRes;
            }
            errors.add("Gmail OAuth: " + gmailRes.error());
        }

        // 2. Try configured SMTP (with 587 <-> 465 SSL failover for reliability)
        String smtpHost = nonEmpty(str(cfg.get("smtpHost")), "smtp.gmail.com");
        int smtpPort = cfg.get("smtpPort") instanceof Number ? ((Number) cfg.get("smtpPort")).intValue() : 587;
        String smtpUser = str(cfg.get("smtpUser"));
        String smtpPass = str(cfg.get("smtpPass")).replaceAll("\\s+", "");

        if (!smtpUser.isEmpty() && !smtpPass.isEmpty()) {
            EmailDeliveryResult primary = sendViaSmtp(smtpHost, smtpPort, smtpUser, smtpPass, fromAddr, cleanTo, subject, html, plainText);
            if (primary.success()) {
                return primary;
            }
            errors.add("SMTP(" + smtpHost + ":" + smtpPort + "): " + primary.error());

            int altPort = (smtpPort == 465) ? 587 : 465;
            EmailDeliveryResult failover = sendViaSmtp(smtpHost, altPort, smtpUser, smtpPass, fromAddr, cleanTo, subject, html, plainText);
            if (failover.success()) {
                return failover;
            }
            errors.add("SMTP(" + smtpHost + ":" + altPort + "): " + failover.error());
        }

        // 3. Try Resend REST API fallback if API key configured
        String resendKey = str(cfg.get("resendApiKey"));
        if (!resendKey.isEmpty()) {
            EmailDeliveryResult resend = sendViaResend(resendKey, fromAddr, cleanTo, subject, html, plainText);
            if (resend.success()) {
                return resend;
            }
            errors.add("Resend: " + resend.error());
        }

        if (errors.isEmpty()) {
            String errMsg = "Email transport is not configured. Please set SMTP_USER / SMTP_PASS or connect Gmail / Resend in Admin -> Communications Settings.";
            logger.warn("[Mail] {}", errMsg);
            return new EmailDeliveryResult(false, "none", null, errMsg);
        }

        String combinedErr = String.join(" | ", errors);
        logger.error("[Mail] All providers failed for {}: {}", cleanTo, combinedErr);
        return new EmailDeliveryResult(false, "failed", null, combinedErr);
    }

    private EmailDeliveryResult sendViaSmtp(String host, int port, String user, String pass,
                                            String fromAddr, String to, String subject, String html, String plainText) {
        try {
            JavaMailSenderImpl sender = getOrCreateMailSender(host, port, user, pass);
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom("Social Shuffle Pune <" + (fromAddr.contains("@") ? fromAddr : user) + ">");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plainText != null ? plainText : stripHtml(html), html);

            sender.send(mimeMessage);
            String msgId = mimeMessage.getMessageID();
            logger.info("[Mail:SMTP] Sent '{}' to {} (port={})", subject, to, port);
            return new EmailDeliveryResult(true, "smtp", msgId != null ? msgId : "smtp-" + System.currentTimeMillis(), null);
        } catch (Exception ex) {
            return new EmailDeliveryResult(false, "smtp", null, ex.getMessage());
        }
    }

    private JavaMailSenderImpl getOrCreateMailSender(String host, int port, String user, String pass) {
        String fingerprint = host + "|" + port + "|" + user + "|" + pass.hashCode();
        if (cachedMailSender != null && fingerprint.equals(lastSenderFingerprint)) {
            return cachedMailSender;
        }
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(host);
        impl.setPort(port);
        impl.setUsername(user);
        impl.setPassword(pass);
        impl.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties props = impl.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "6000");
        props.put("mail.smtp.timeout", "8000");
        props.put("mail.smtp.writetimeout", "8000");
        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "false");
        }
        cachedMailSender = impl;
        lastSenderFingerprint = fingerprint;
        return impl;
    }

    private EmailDeliveryResult sendViaGmailApi(String accessToken, String gmailFrom, String to, String subject, String html) {
        try {
            JavaMailSenderImpl tmp = new JavaMailSenderImpl();
            MimeMessage msg = tmp.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, StandardCharsets.UTF_8.name());
            if (gmailFrom != null && gmailFrom.contains("@")) {
                helper.setFrom("Social Shuffle Pune <" + gmailFrom + ">");
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            msg.saveChanges();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            msg.writeTo(baos);
            String rawEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());

            String jsonBody = objectMapper.writeValueAsString(Map.of("raw", rawEncoded));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                Map<?, ?> parsed = objectMapper.readValue(resp.body(), Map.class);
                String id = parsed.get("id") != null ? String.valueOf(parsed.get("id")) : "gmail-sent";
                return new EmailDeliveryResult(true, "gmail-oauth2", id, null);
            }
            return new EmailDeliveryResult(false, "gmail-oauth2", null, "HTTP " + resp.statusCode() + ": " + resp.body());
        } catch (Exception ex) {
            return new EmailDeliveryResult(false, "gmail-oauth2", null, ex.getMessage());
        }
    }

    private EmailDeliveryResult sendViaResend(String apiKey, String fromAddr, String to, String subject, String html, String plainText) {
        try {
            String from = fromAddr.contains("@") ? ("Social Shuffle Pune <" + fromAddr + ">") : "Social Shuffle Pune <onboarding@resend.dev>";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from", from);
            body.put("to", Collections.singletonList(to));
            body.put("subject", subject);
            body.put("html", html);
            body.put("text", plainText != null ? plainText : stripHtml(html));

            String json = objectMapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                Map<?, ?> parsed = objectMapper.readValue(resp.body(), Map.class);
                String id = parsed.get("id") != null ? String.valueOf(parsed.get("id")) : "resend-sent";
                return new EmailDeliveryResult(true, "resend", id, null);
            }
            return new EmailDeliveryResult(false, "resend", null, "HTTP " + resp.statusCode() + ": " + resp.body());
        } catch (Exception ex) {
            return new EmailDeliveryResult(false, "resend", null, ex.getMessage());
        }
    }

    // =========================================================================
    // Synchronous OTP & Test Emails
    // =========================================================================

    public EmailDeliveryResult sendOtpEmailSync(String toEmail, String name, String otp) {
        String safeName = escapeHtml(nonEmpty(name, "Fellow Shuffler"));
        String safeOtp = escapeHtml(otp);
        String subject = otp + " is your Social Shuffle Pune verification code";
        String html = "<div style='font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;max-width:560px;margin:20px auto;background:#0f172a;color:#e2e8f0;border:1px solid #334155;border-radius:16px;overflow:hidden'>"
                + "<div style='background:linear-gradient(135deg,#FF6036,#FF751F);padding:28px 24px;text-align:center;color:#fff'>"
                + "<div style='font-size:12px;font-weight:800;text-transform:uppercase;letter-spacing:1.2px;margin-bottom:6px'>Social Shuffle Pune</div>"
                + "<h1 style='margin:0;font-size:24px;font-weight:900'>Verify Your Email</h1></div>"
                + "<div style='padding:28px 24px'>"
                + "<p style='margin-top:0;font-size:15px'>Hey <strong>" + safeName + "</strong>,</p>"
                + "<p style='font-size:14px;color:#cbd5e1;line-height:1.6'>Use the 6-digit one-time verification code below to complete your Social Shuffle registration:</p>"
                + "<div style='margin:24px 0;padding:20px;background:#1e293b;border:2px dashed #FF751F;border-radius:12px;text-align:center'>"
                + "<div style='font-family:monospace;font-size:36px;font-weight:900;letter-spacing:8px;color:#FFC226'>" + safeOtp + "</div>"
                + "<div style='font-size:12px;color:#94a3b8;margin-top:8px'>Valid for 10 minutes • Never share this code</div></div>"
                + "<p style='font-size:12px;color:#64748b;margin-bottom:0'>If you didn't request this code, you can safely ignore this email.</p>"
                + "</div></div>";
        String plain = "Hey " + nonEmpty(name, "Fellow Shuffler") + ",\nYour Social Shuffle Pune verification code is: " + otp + "\nValid for 10 minutes.";
        return dispatchEmail(toEmail, subject, html, plain);
    }

    // =========================================================================
    // Non-Blocking Async Lifecycle Emails
    // =========================================================================

    @Async("mailTaskExecutor")
    public void sendWelcomeEmailAsync(String toEmail, String name) {
        try {
            String safeName = escapeHtml(nonEmpty(name, "Shuffler"));
            String subject = "Welcome to Social Shuffle Pune, " + nonEmpty(name, "Shuffler") + "!";
            String html = "<div style='font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;max-width:580px;margin:20px auto;background:#0f172a;color:#e2e8f0;border:1px solid #334155;border-radius:16px;overflow:hidden'>"
                    + "<div style='background:linear-gradient(135deg,#0284c7,#7c3aed);padding:32px 24px;text-align:center;color:#fff'>"
                    + "<h1 style='margin:0;font-size:26px;font-weight:900'>Welcome to the Table, " + safeName + "!</h1></div>"
                    + "<div style='padding:28px 24px;font-size:14px;line-height:1.65;color:#cbd5e1'>"
                    + "<p>Your Shuffler account is now active! You can RSVP for Pune weekend board game meetups, track games played, unlock badges, and carry your QR check-in pass.</p>"
                    + "<p style='margin-bottom:0'>See you at the next meetup across Koregaon Park, Baner, Viman Nagar, or FC Road!</p>"
                    + "</div></div>";
            dispatchEmail(toEmail, subject, html, "Welcome to Social Shuffle Pune, " + nonEmpty(name, "Shuffler") + "! Your account is active.");
        } catch (Exception e) {
            logger.warn("Async welcome email failed for {}: {}", toEmail, e.getMessage());
        }
    }

    @Async("mailTaskExecutor")
    public void sendRegistrationConfirmationAsync(Registration registration, ShuffleEvent event) {
        if (registration == null || registration.getParticipantEmail() == null || registration.getParticipantEmail().trim().isEmpty()) {
            return;
        }
        EmailDeliveryResult res = sendRegistrationConfirmationSync(registration, event);
        if (res.success()) {
            try {
                registrationRepository.findById(registration.getId()).ifPresent(existing -> {
                    existing.setEmailSent(true);
                    existing.setEmailSentAt(Instant.now().toString());
                    registrationRepository.save(existing);
                });
            } catch (Exception ignored) {
            }
        }
    }

    public EmailDeliveryResult sendRegistrationConfirmationSync(Registration registration, ShuffleEvent event) {
        String eventTitle = event != null && event.getTitle() != null ? event.getTitle() : "Social Shuffle Meetup";
        String html = generateQuirkyRegistrationEmailHtml(registration, event);
        String subject = "Registration Pass Confirmed: " + eventTitle;
        return dispatchEmail(registration.getParticipantEmail(), subject, html, "Your Social Shuffle registration pass (" + registration.getQrCodeToken() + ") for " + eventTitle + " is confirmed.");
    }

    @Async("mailTaskExecutor")
    public void sendVolunteerConfirmationAndAdminAlertAsync(VolunteerApplication app) {
        if (app == null) return;
        try {
            if (app.getEmail() != null && app.getEmail().contains("@")) {
                String safeName = escapeHtml(nonEmpty(app.getName(), "Volunteer"));
                String html = "<div style='font-family:sans-serif;max-width:560px;margin:20px auto;background:#0f172a;color:#e2e8f0;padding:28px;border-radius:16px;border:1px solid #334155'>"
                        + "<h2 style='color:#FFC226;margin-top:0'>Volunteer Application Received!</h2>"
                        + "<p>Hey <strong>" + safeName + "</strong>, thank you for applying to volunteer with Social Shuffle Pune.</p>"
                        + "<p>Our organizers (Parimal & Diksha) will review your application for <strong>" + escapeHtml(nonEmpty(app.getArea(), "Pune")) + "</strong> and get in touch shortly.</p>"
                        + "</div>";
                dispatchEmail(app.getEmail(), "Volunteer Application Received - Social Shuffle Pune", html, null);
            }
            String adminEmail = resolveAdminEmail();
            if (!adminEmail.isEmpty()) {
                String adminHtml = "<div style='font-family:sans-serif;max-width:560px;margin:20px auto;background:#0f172a;color:#e2e8f0;padding:24px;border-radius:12px'>"
                        + "<h2 style='color:#35C8A8;margin-top:0'>New Volunteer Application</h2>"
                        + "<p><strong>Name:</strong> " + escapeHtml(app.getName()) + "</p>"
                        + "<p><strong>Email:</strong> " + escapeHtml(app.getEmail()) + "</p>"
                        + "<p><strong>Phone:</strong> " + escapeHtml(app.getPhone()) + "</p>"
                        + "<p><strong>Area:</strong> " + escapeHtml(app.getArea()) + "</p>"
                        + "<p><strong>Experience:</strong> " + escapeHtml(app.getExperience()) + "</p>"
                        + "</div>";
                dispatchEmail(adminEmail, "New Volunteer Application: " + nonEmpty(app.getName(), "Applicant"), adminHtml, null);
            }
        } catch (Exception e) {
            logger.warn("Async volunteer notification failed: {}", e.getMessage());
        }
    }

    @Async("mailTaskExecutor")
    public void sendSafetyReportAdminAlertAsync(SafetyReport report) {
        if (report == null) return;
        try {
            String adminEmail = resolveAdminEmail();
            if (!adminEmail.isEmpty()) {
                String adminHtml = "<div style='font-family:sans-serif;max-width:560px;margin:20px auto;background:#0f172a;color:#e2e8f0;padding:24px;border-radius:12px;border:1px solid #ef4444'>"
                        + "<h2 style='color:#f87171;margin-top:0'>Safety Report Submitted</h2>"
                        + "<p><strong>Type:</strong> " + escapeHtml(nonEmpty(report.getType(), report.getCategory())) + "</p>"
                        + "<p><strong>Event:</strong> " + escapeHtml(nonEmpty(report.getEventTitle(), report.getEventId())) + "</p>"
                        + "<p><strong>Description:</strong> " + escapeHtml(report.getDescription()) + "</p>"
                        + "<p><strong>Anonymous:</strong> " + (report.isAnonymous() ? "Yes" : "No (" + escapeHtml(report.getReporterName()) + ")") + "</p>"
                        + "</div>";
                dispatchEmail(adminEmail, "[Safety Alert] New Community Report", adminHtml, null);
            }
        } catch (Exception e) {
            logger.warn("Async safety report notification failed: {}", e.getMessage());
        }
    }

    @Async("mailTaskExecutor")
    public void sendFeedbackAdminAlertAsync(EventFeedback feedback) {
        if (feedback == null) return;
        try {
            String adminEmail = resolveAdminEmail();
            if (!adminEmail.isEmpty()) {
                String html = "<div style='font-family:sans-serif;max-width:560px;margin:20px auto;background:#0f172a;color:#e2e8f0;padding:24px;border-radius:12px'>"
                        + "<h2 style='color:#FFC226;margin-top:0'>New Meetup Feedback</h2>"
                        + "<p><strong>Event:</strong> " + escapeHtml(nonEmpty(feedback.getEventTitle(), feedback.getEventId())) + "</p>"
                        + "<p><strong>Rating:</strong> " + feedback.getOverallRating() + "/5</p>"
                        + "<p><strong>From:</strong> " + (feedback.isAnonymous() ? "Anonymous" : escapeHtml(feedback.getParticipantName())) + "</p>"
                        + "<p><strong>Suggestions:</strong> " + escapeHtml(feedback.getSuggestions()) + "</p>"
                        + "</div>";
                dispatchEmail(adminEmail, "Meetup Feedback Received (" + feedback.getOverallRating() + "/5)", html, null);
            }
        } catch (Exception e) {
            logger.warn("Async feedback notification failed: {}", e.getMessage());
        }
    }

    // =========================================================================
    // Existing Sync Helpers Upgraded to Real Dispatch
    // =========================================================================

    public boolean sendRegistrationConfirmationEmail(Registration registration, ShuffleEvent event) {
        String qrToken = registration.getQrCodeToken() != null ? registration.getQrCodeToken() : ("SS-REG-" + registration.getId());
        registration.setQrCodeToken(qrToken);
        registration.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + qrToken);
        EmailDeliveryResult res = sendRegistrationConfirmationSync(registration, event);
        if (res.success()) {
            registration.setEmailSent(true);
            registration.setEmailSentAt(Instant.now().toString());
        }
        return res.success();
    }

    public boolean sendEventReminderEmail(Registration registration, ShuffleEvent event, String customNote) {
        String html = generateUpcomingEventReminderEmailHtml(registration, event, customNote);
        String subject = "Reminder: " + (event != null && event.getTitle() != null ? event.getTitle() : "Social Shuffle Meetup");
        return dispatchEmail(registration.getParticipantEmail(), subject, html, null).success();
    }

    public boolean sendEventBroadcastEmail(Registration registration, ShuffleEvent event, String subject, String message, boolean includeQrPass) {
        String html = generateEventBroadcastEmailHtml(registration, event, subject, message, includeQrPass);
        return dispatchEmail(registration.getParticipantEmail(), subject, html, message).success();
    }

    public boolean sendCommunityAnnouncementEmail(String email, String name, String title, String message) {
        String html = generateCommunityAnnouncementEmailHtml(name, title, message);
        return dispatchEmail(email, title, html, message).success();
    }

    // =========================================================================
    // HTML Template Generators
    // =========================================================================

    public String generateQuirkyRegistrationEmailHtml(Registration registration, ShuffleEvent event) {
        String recipientName = escapeHtml(registration.getParticipantName() != null ? registration.getParticipantName() : "Fellow Shuffler");
        String eventTitle = escapeHtml(event != null && event.getTitle() != null ? event.getTitle() : "Pune Board Game Shuffler Meetup");
        String eventVenue = escapeHtml(event != null && event.getVenue() != null ? event.getVenue() : "Pune Meetup Hub");
        String eventAddress = escapeHtml(event != null && event.getAddress() != null ? event.getAddress() : "Pune, Maharashtra");
        String eventTime = escapeHtml(event != null && event.getTime() != null ? event.getTime() : "Check app schedule");
        int pax = registration.getPaxCount() > 0 ? registration.getPaxCount() : 1;
        String qrToken = registration.getQrCodeToken() != null ? registration.getQrCodeToken() : ("SS-REG-" + registration.getId());

        String encodedQrData = URLEncoder.encode(qrToken, StandardCharsets.UTF_8);
        String qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=240x240&margin=10&data=" + encodedQrData;

        String guestListHtml = "";
        if (registration.getGuests() != null && !registration.getGuests().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (GuestInfo g : registration.getGuests()) {
                if (g.getName() != null && !g.getName().trim().isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(escapeHtml(g.getName().trim()));
                }
            }
            if (sb.length() > 0) {
                guestListHtml = "<div class='field'><span class='label'>Accompanying Guests:</span><span class='val' style='color:#c084fc'>" + sb + "</span></div>";
            }
        }

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>"
                + "body{margin:0;padding:0;background:#0f111a;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#e2e8f0}"
                + ".container{max-width:600px;margin:24px auto;background:#161824;border:1px solid #2d3748;border-radius:16px;overflow:hidden}"
                + ".header{background:linear-gradient(135deg,#7c3aed,#4f46e5);padding:32px 24px;text-align:center;color:#fff}"
                + ".body{padding:28px}.card{background:#1e2235;border:1px solid #333a52;border-radius:12px;padding:18px;margin-bottom:20px}"
                + ".field{display:flex;justify-content:space-between;margin-bottom:10px;font-size:13px}"
                + ".label{color:#94a3b8;font-weight:600}.val{color:#f8fafc;font-weight:700;text-align:right}"
                + ".qr{text-align:center;background:#0b0d14;border:2px dashed #6366f1;border-radius:16px;padding:24px}"
                + "</style></head><body><div class='container'>"
                + "<div class='header'><h1 style='margin:0;font-size:24px;font-weight:900'>Holy Meeples, " + recipientName + "! You're In!</h1></div>"
                + "<div class='body'><div class='card'>"
                + "<div class='field'><span class='label'>Meetup:</span><span class='val'>" + eventTitle + "</span></div>"
                + "<div class='field'><span class='label'>When:</span><span class='val'>" + eventTime + "</span></div>"
                + "<div class='field'><span class='label'>Where:</span><span class='val'>" + eventVenue + " (" + eventAddress + ")</span></div>"
                + "<div class='field'><span class='label'>Seats (PAX):</span><span class='val'>" + pax + " Shuffler(s)</span></div>"
                + guestListHtml
                + "<div class='field'><span class='label'>Payment:</span><span class='val' style='color:#4ade80'>" + escapeHtml(registration.getPaymentStatus()) + "</span></div>"
                + "<div class='field'><span class='label'>Registration ID:</span><span class='val' style='font-family:monospace'>" + escapeHtml(registration.getId()) + "</span></div>"
                + "</div><div class='qr'><h4 style='margin:0 0 10px 0;color:#fff'>Digital Check-in Pass</h4>"
                + "<img src='" + qrImageUrl + "' width='200' height='200' style='border-radius:10px;background:#fff;padding:6px' />"
                + "<div style='font-family:monospace;color:#a855f7;font-weight:700;margin-top:10px'>" + escapeHtml(qrToken) + "</div>"
                + "</div></div></div></body></html>";
    }

    public String generateUpcomingEventReminderEmailHtml(Registration registration, ShuffleEvent event, String customNote) {
        String recipientName = escapeHtml(registration.getParticipantName() != null ? registration.getParticipantName() : "Fellow Shuffler");
        String eventTitle = escapeHtml(event != null && event.getTitle() != null ? event.getTitle() : "Upcoming Pune Board Game Meetup");
        String eventVenue = escapeHtml(event != null && event.getVenue() != null ? event.getVenue() : "Pune Venue");
        String eventTime = escapeHtml(event != null && event.getTime() != null ? event.getTime() : "Check app schedule");
        String qrToken = registration.getQrCodeToken() != null ? registration.getQrCodeToken() : ("SS-REG-" + registration.getId());
        String qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=240x240&margin=10&data=" + URLEncoder.encode(qrToken, StandardCharsets.UTF_8);

        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#080D1F;font-family:sans-serif;color:#e2e8f0'>"
                + "<div style='max-width:600px;margin:20px auto;background:#0E162B;border:1px solid #1F305E;border-radius:16px;overflow:hidden'>"
                + "<div style='background:linear-gradient(135deg,#FF6036,#D946EF);padding:28px 24px;text-align:center;color:#fff'>"
                + "<h1 style='margin:0;font-size:24px'>Get Ready, " + recipientName + "!</h1><p style='margin:6px 0 0'>" + eventTitle + "</p></div>"
                + "<div style='padding:24px'>"
                + (customNote != null && !customNote.trim().isEmpty() ? "<p style='background:rgba(255,96,54,0.12);border-left:4px solid #FF6036;padding:12px;border-radius:6px'>" + escapeHtml(customNote) + "</p>" : "")
                + "<p><strong>Venue:</strong> " + eventVenue + "<br/><strong>Time:</strong> " + eventTime + "</p>"
                + "<div style='text-align:center;padding:16px;background:#070B17;border:2px dashed #FF6036;border-radius:12px'>"
                + "<img src='" + qrImageUrl + "' width='180' height='180' style='background:#fff;border-radius:8px' />"
                + "<div style='font-family:monospace;color:#FF6036;margin-top:8px'>" + escapeHtml(qrToken) + "</div></div></div></div></body></html>";
    }

    public String generateEventBroadcastEmailHtml(Registration registration, ShuffleEvent event, String subject, String message, boolean includeQrPass) {
        String recipientName = escapeHtml(registration.getParticipantName() != null ? registration.getParticipantName() : "Fellow Shuffler");
        String eventTitle = escapeHtml(event != null && event.getTitle() != null ? event.getTitle() : "Social Shuffle Meetup");
        String eventVenue = escapeHtml(event != null && event.getVenue() != null ? event.getVenue() : "Pune Venue");
        String eventTime = escapeHtml(event != null && event.getTime() != null ? event.getTime() : "Event Date");
        int pax = registration.getPaxCount() > 0 ? registration.getPaxCount() : 1;
        String qrToken = registration.getQrCodeToken() != null ? registration.getQrCodeToken() : ("SS-REG-" + registration.getId());
        String qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=240x240&margin=10&data=" + URLEncoder.encode(qrToken, StandardCharsets.UTF_8);

        String parsedMessage = escapeHtml(message != null ? message : "")
                .replace("{name}", recipientName)
                .replace("{event_title}", eventTitle)
                .replace("{event_venue}", eventVenue)
                .replace("{event_time}", eventTime)
                .replace("{pax_count}", String.valueOf(pax))
                .replace("{qr_code}", escapeHtml(qrToken))
                .replace("\n", "<br/>");

        String qrBlock = includeQrPass
                ? "<div style='text-align:center;padding:16px;margin-top:16px;background:#070B17;border:2px dashed #35C8A8;border-radius:12px'><img src='" + qrImageUrl + "' width='160' height='160' style='background:#fff;border-radius:8px'/><div style='font-family:monospace;color:#35C8A8;margin-top:8px'>" + escapeHtml(qrToken) + "</div></div>"
                : "";

        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#080D1F;font-family:sans-serif;color:#e2e8f0'>"
                + "<div style='max-width:600px;margin:20px auto;background:#0E162B;border:1px solid #1F305E;border-radius:16px;overflow:hidden'>"
                + "<div style='background:linear-gradient(135deg,#1E1B4B,#4338CA);padding:28px 24px;text-align:center;color:#fff'>"
                + "<h1 style='margin:0;font-size:22px'>" + escapeHtml(subject) + "</h1><p style='margin:6px 0 0'>" + eventTitle + " • " + eventVenue + "</p></div>"
                + "<div style='padding:24px'><p>Hello <strong>" + recipientName + "</strong>,</p><div style='background:#131E3A;border-left:4px solid #35C8A8;padding:16px;border-radius:8px'>" + parsedMessage + "</div>"
                + qrBlock + "</div></div></body></html>";
    }

    public String generateCommunityAnnouncementEmailHtml(String recipientName, String title, String message) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#080D1F;font-family:sans-serif;color:#e2e8f0'>"
                + "<div style='max-width:600px;margin:20px auto;background:#0E162B;border:1px solid #1F305E;border-radius:16px;overflow:hidden'>"
                + "<div style='background:linear-gradient(135deg,#0284c7,#0369a1);padding:28px 24px;text-align:center;color:#fff'>"
                + "<h1 style='margin:0;font-size:22px'>" + escapeHtml(title) + "</h1></div>"
                + "<div style='padding:24px'><p>Hey <strong>" + escapeHtml(nonEmpty(recipientName, "Shuffler")) + "</strong>,</p>"
                + "<div style='background:#131E3A;border-left:4px solid #38bdf8;padding:16px;border-radius:8px'>" + escapeHtml(message != null ? message : "").replace("\n", "<br/>") + "</div>"
                + "</div></div></body></html>";
    }

    private String resolveFromAddress(Map<String, Object> cfg) {
        String from = str(cfg.get("smtpFrom"));
        if (!from.isEmpty()) return from;
        String user = str(cfg.get("smtpUser"));
        if (!user.isEmpty()) return user;
        String gmail = str(cfg.get("gmailEmail"));
        return !gmail.isEmpty() ? gmail : "noreply@socialshufflepune.com";
    }

    private String resolveAdminEmail() {
        Map<String, Object> cfg = appConfigService.getCommunicationsConfig();
        return nonEmpty(str(cfg.get("adminNotificationEmail")), "parimalmshete@gmail.com");
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String stripHtml(String html) {
        return html == null ? "" : html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    private String nonEmpty(String val, String fallback) {
        return (val != null && !val.trim().isEmpty()) ? val.trim() : fallback;
    }

    private String str(Object obj) {
        return obj == null ? "" : String.valueOf(obj).trim();
    }
}

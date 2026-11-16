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
            String errMsg = "SMTP password (SMTP_PASS) for socialshuffle.pune@gmail.com is not configured yet. Please set SMTP_PASS or add the Gmail App Password / Connect Gmail in Admin -> Mail & OTP Settings.";
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
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.ssl.trust", "*");
        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
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
        String primaryFrom = (fromAddr != null && fromAddr.contains("@") && !fromAddr.equalsIgnoreCase("noreply@socialshufflepune.com"))
                ? ("Social Shuffle Pune <" + fromAddr + ">")
                : "Social Shuffle Pune <onboarding@resend.dev>";
        EmailDeliveryResult first = sendResendHttp(apiKey, primaryFrom, to, subject, html, plainText);
        if (!first.success() && !primaryFrom.contains("onboarding@resend.dev")
                && first.error() != null && first.error().contains("403")) {
            return sendResendHttp(apiKey, "Social Shuffle Pune <onboarding@resend.dev>", to, subject, html, plainText);
        }
        return first;
    }

    private EmailDeliveryResult sendResendHttp(String apiKey, String fromHeader, String to, String subject, String html, String plainText) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from", fromHeader);
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
    // Synchronous OTP & Diagnostic Test Emails
    // =========================================================================

    public EmailDeliveryResult sendOtpEmailSync(String toEmail, String name, String otp) {
        String safeName = escapeHtml(nonEmpty(name, "Fellow Shuffler"));
        String safeOtp = escapeHtml(otp);
        String subject = otp + " is your Social Shuffle Pune verification code";

        String body = "<p style='margin:0 0 14px 0;font-size:15px;color:#F8FAFC;'>Hey <strong>" + safeName + "</strong>,</p>"
                + "<p style='margin:0 0 20px 0;font-size:14px;line-height:1.65;color:#CBD5E1;'>"
                + "Enter the 6-digit verification code below to verify your email address and activate your Social Shuffle Pune account:</p>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='margin:0 0 20px 0;background-color:#10173E;border:2px dashed #FF751F;border-radius:14px;'>"
                + "<tr><td align='center' style='padding:22px 16px;'>"
                + "<div style='font-size:11px;font-weight:700;letter-spacing:1.6px;text-transform:uppercase;color:#FF751F;margin-bottom:8px;'>One-Time Verification Code</div>"
                + "<div style='font-family:ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,monospace;font-size:36px;font-weight:900;letter-spacing:10px;color:#FFD230;'>" + safeOtp + "</div>"
                + "<div style='font-size:12px;color:#94A3B8;margin-top:10px;'>Expires in <strong style='color:#F8FAFC;'>10 minutes</strong> &bull; Never share this code</div>"
                + "</td></tr></table>"
                + "<p style='margin:0;font-size:12px;line-height:1.55;color:#94A3B8;'>Didn&rsquo;t request this verification code? You can safely ignore this message&mdash;no changes were made to your account.</p>";

        String html = buildThemedEmail(
                "SECURITY VERIFICATION",
                "Verify Your Email Address",
                "Complete your Social Shuffle Pune sign-up",
                "#FF751F",
                "#FFD230",
                body,
                "Automated security verification from Social Shuffle Pune."
        );
        String plain = "Hey " + nonEmpty(name, "Fellow Shuffler") + ",\nYour Social Shuffle Pune verification code is: " + otp + "\nValid for 10 minutes.";
        return dispatchEmail(toEmail, subject, html, plain);
    }

    public String generateDiagnosticTestEmailHtml(String recipientEmail, String subject, String message) {
        String body = "<p style='margin:0 0 14px 0;font-size:14px;line-height:1.65;color:#CBD5E1;'>"
                + "This diagnostic email confirms your outbound mail pipeline is active and delivering messages with the Social Shuffle Pune theme.</p>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:#1A255C;border:1px solid #283677;border-radius:12px;margin-bottom:18px;'>"
                + buildDetailRow("Sender Account", "socialshuffle.pune@gmail.com", "#4AB4A4")
                + buildDetailRow("Test Recipient", escapeHtml(nonEmpty(recipientEmail, "parimalmshete@gmail.com")), "#F8FAFC")
                + buildDetailRow("Subject", escapeHtml(nonEmpty(subject, "Test Email")), "#FFD230")
                + buildDetailRow("Dispatched At", escapeHtml(Instant.now().toString()), "#CBD5E1")
                + "</table>"
                + "<div style='background-color:#10173E;border-left:4px solid #4AB4A4;border-radius:8px;padding:14px 16px;font-size:13px;line-height:1.6;color:#E2E8F0;'>"
                + escapeHtml(nonEmpty(message, "Email delivery verified.")).replace("\n", "<br/>")
                + "</div>";

        return buildThemedEmail(
                "TRANSPORT DIAGNOSTIC",
                escapeHtml(nonEmpty(subject, "Mail Transport Verified")),
                "Admin Mail & OTP Settings Verification",
                "#4AB4A4",
                "#FF751F",
                body,
                "Sent via Social Shuffle Admin Mail Communications Hub."
        );
    }

    // =========================================================================
    // Non-Blocking Async Lifecycle Emails
    // =========================================================================

    @Async("mailTaskExecutor")
    public void sendWelcomeEmailAsync(String toEmail, String name) {
        try {
            String safeName = escapeHtml(nonEmpty(name, "Shuffler"));
            String subject = "Welcome to Social Shuffle Pune, " + nonEmpty(name, "Shuffler") + "!";
            String body = "<p style='margin:0 0 14px 0;font-size:15px;color:#F8FAFC;'>Hey <strong>" + safeName + "</strong>,</p>"
                    + "<p style='margin:0 0 18px 0;font-size:14px;line-height:1.65;color:#CBD5E1;'>"
                    + "Your Shuffler profile is officially live! Pull up a chair at Pune&rsquo;s friendliest tabletop board game community.</p>"
                    + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:#1A255C;border:1px solid #283677;border-radius:12px;margin-bottom:16px;'>"
                    + buildDetailRow("Weekend Meetups", "RSVP across Koregaon Park, Baner, Viman Nagar & FC Road", "#FF751F")
                    + buildDetailRow("Digital Check-In", "Instant QR pass issued automatically for every registration", "#4AB4A4")
                    + buildDetailRow("Shuffler Progression", "Track games played & unlock all 10 collectible tabletop badges", "#FFD230")
                    + "</table>"
                    + "<p style='margin:0;font-size:13px;color:#94A3B8;'>See you across the board this weekend!</p>";

            String html = buildThemedEmail(
                    "WELCOME TO THE TABLE",
                    "Welcome, " + safeName + "!",
                    "Your Social Shuffle Pune Shuffler Passport is ready",
                    "#4AB4A4",
                    "#FF751F",
                    body,
                    "Need help finding a beginner-friendly table? Reply to socialshuffle.pune@gmail.com."
            );
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
                String safeArea = escapeHtml(nonEmpty(app.getArea(), "Pune"));
                String body = "<p style='margin:0 0 14px 0;font-size:15px;color:#F8FAFC;'>Hey <strong>" + safeName + "</strong>,</p>"
                        + "<p style='margin:0 0 18px 0;font-size:14px;line-height:1.65;color:#CBD5E1;'>"
                        + "Thank you for putting your hand up to host tables and welcome players with Social Shuffle Pune!</p>"
                        + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:#1A255C;border:1px solid #283677;border-radius:12px;margin-bottom:16px;'>"
                        + buildDetailRow("Preferred Area", safeArea, "#4AB4A4")
                        + buildDetailRow("Application Status", "Under Organizer Review", "#FFD230")
                        + buildDetailRow("Review Team", "Parimal &amp; Diksha (Community Leads)", "#F8FAFC")
                        + "</table>"
                        + "<p style='margin:0;font-size:13px;line-height:1.6;color:#94A3B8;'>We&rsquo;ll reach out on WhatsApp or email before the next weekend meetup in your area.</p>";

                String html = buildThemedEmail(
                        "VOLUNTEER APPLICATION",
                        "Application Received!",
                        "Thanks for helping Pune gather & play",
                        "#4AB4A4",
                        "#FFD230",
                        body,
                        "Volunteer Desk &bull; Social Shuffle Pune"
                );
                dispatchEmail(app.getEmail(), "Volunteer Application Received - Social Shuffle Pune", html, null);
            }

            String adminEmail = resolveAdminEmail();
            if (!adminEmail.isEmpty()) {
                String adminBody = "<p style='margin:0 0 14px 0;font-size:13px;color:#CBD5E1;'>A new community volunteer application was submitted and awaits organizer review in the Admin Console:</p>"
                        + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:#1A255C;border:1px solid #283677;border-radius:12px;'>"
                        + buildDetailRow("Applicant Name", escapeHtml(nonEmpty(app.getName(), "Applicant")), "#F8FAFC")
                        + buildDetailRow("Email", escapeHtml(nonEmpty(app.getEmail(), "-")), "#4AB4A4")
                        + buildDetailRow("Phone", escapeHtml(nonEmpty(app.getPhone(), "-")), "#FFD230")
                        + buildDetailRow("Pune Area", escapeHtml(nonEmpty(app.getArea(), "Pune")), "#F8FAFC")
                        + buildDetailRow("Experience", escapeHtml(nonEmpty(app.getExperience(), "Not specified")), "#CBD5E1")
                        + "</table>";

                String adminHtml = buildThemedEmail(
                        "ADMIN • VOLUNTEER DESK",
                        "New Volunteer Application",
                        escapeHtml(nonEmpty(app.getName(), "Applicant")) + " applied to host/volunteer",
                        "#FFD230",
                        "#4AB4A4",
                        adminBody,
                        "Review and approve in Admin Console &rarr; Volunteer Applications."
                );
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
                String reporterLabel = report.isAnonymous()
                        ? "Anonymous Reporter"
                        : escapeHtml(nonEmpty(report.getReporterName(), "Participant"));
                String adminBody = "<p style='margin:0 0 14px 0;font-size:13px;color:#FECDD3;'>A community safety or grievance report requires organizer attention:</p>"
                        + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:#1A255C;border:1px solid #EF4444;border-radius:12px;margin-bottom:16px;'>"
                        + buildDetailRow("Report Type", escapeHtml(nonEmpty(report.getType(), report.getCategory())), "#FCA5A5")
                        + buildDetailRow("Meetup / Event", escapeHtml(nonEmpty(report.getEventTitle(), report.getEventId())), "#F8FAFC")
                        + buildDetailRow("Submitted By", reporterLabel, "#FFD230")
                        + "</table>"
                        + "<div style='background-color:#10173E;border-left:4px solid #EF4444;border-radius:8px;padding:14px 16px;font-size:13px;line-height:1.6;color:#F8FAFC;'>"
                        + "<div style='font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:1px;color:#FCA5A5;margin-bottom:6px;'>Incident Details</div>"
                        + escapeHtml(nonEmpty(report.getDescription(), "No description provided.")).replace("\n", "<br/>")
                        + "</div>";

                String adminHtml = buildThemedEmail(
                        "PRIORITY • SAFETY DESK",
                        "Safety & Grievance Report",
                        "Immediate organizer review requested",
                        "#EF4444",
                        "#FF751F",
                        adminBody,
                        "Manage resolution status in Admin Console &rarr; Safety &amp; Reports."
                );
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
                int rating = Math.max(0, Math.min(5, feedback.getOverallRating()));
                String stars = "★".repeat(rating) + "☆".repeat(5 - rating);
                String body = "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='margin:0 0 16px 0;background-color:#10173E;border:1px solid #9440DD;border-radius:12px;'>"
                        + "<tr><td align='center' style='padding:16px;'>"
                        + "<div style='font-size:24px;letter-spacing:4px;color:#FFD230;'>" + stars + "</div>"
                        + "<div style='font-size:12px;font-weight:700;color:#CBD5E1;margin-top:4px;'>Overall Meetup Rating: " + rating + " / 5</div>"
                        + "</td></tr></table>"
                        + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:#1A255C;border:1px solid #283677;border-radius:12px;margin-bottom:16px;'>"
                        + buildDetailRow("Meetup", escapeHtml(nonEmpty(feedback.getEventTitle(), feedback.getEventId())), "#F8FAFC")
                        + buildDetailRow("Attendee", feedback.isAnonymous() ? "Anonymous Shuffler" : escapeHtml(nonEmpty(feedback.getParticipantName(), "Participant")), "#4AB4A4")
                        + "</table>"
                        + "<div style='background-color:#10173E;border-left:4px solid #9440DD;border-radius:8px;padding:14px 16px;font-size:13px;line-height:1.6;color:#E2E8F0;'>"
                        + "<div style='font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:1px;color:#C084FC;margin-bottom:6px;'>Suggestions &amp; Notes</div>"
                        + escapeHtml(nonEmpty(feedback.getSuggestions(), "No additional comments.")).replace("\n", "<br/>")
                        + "</div>";

                String html = buildThemedEmail(
                        "ADMIN • FEEDBACK WALL",
                        "New Meetup Feedback (" + rating + "/5)",
                        escapeHtml(nonEmpty(feedback.getEventTitle(), "Social Shuffle Meetup")),
                        "#9440DD",
                        "#FFD230",
                        body,
                        "View full feedback breakdown in Admin Console &rarr; Feedback Submissions."
                );
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
    // Context-Specific HTML Template Generators (Unified App Theme)
    // =========================================================================

    public String generateQuirkyRegistrationEmailHtml(Registration registration, ShuffleEvent event) {
        String recipientName = escapeHtml(nonEmpty(registration.getParticipantName(), "Fellow Shuffler"));
        String eventTitle = escapeHtml(event != null && event.getTitle() != null ? event.getTitle() : "Pune Board Game Shuffler Meetup");
        String eventVenue = escapeHtml(event != null && event.getVenue() != null ? event.getVenue() : "Pune Meetup Hub");
        String eventAddress = escapeHtml(event != null && event.getAddress() != null ? event.getAddress() : "Pune, Maharashtra");
        String eventTime = escapeHtml(event != null && event.getTime() != null ? event.getTime() : "Check app schedule");
        int pax = registration.getPaxCount() > 0 ? registration.getPaxCount() : 1;
        String qrToken = registration.getQrCodeToken() != null ? registration.getQrCodeToken() : ("SS-REG-" + registration.getId());

        String encodedQrData = URLEncoder.encode(qrToken, StandardCharsets.UTF_8);
        String qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=220x220&margin=10&data=" + encodedQrData;

        String guestListRow = "";
        if (registration.getGuests() != null && !registration.getGuests().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (GuestInfo g : registration.getGuests()) {
                if (g.getName() != null && !g.getName().trim().isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(escapeHtml(g.getName().trim()));
                }
            }
            if (sb.length() > 0) {
                guestListRow = buildDetailRow("Guests", sb.toString(), "#C084FC");
            }
        }

        String body = "<p style='margin:0 0 16px 0;font-size:14px;line-height:1.6;color:#CBD5E1;'>"
                + "Your seat at the table is locked in! Present the QR pass below at the host desk for 1-tap check-in.</p>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:#1A255C;border:1px solid #283677;border-radius:12px;margin-bottom:20px;'>"
                + buildDetailRow("Meetup", eventTitle, "#F8FAFC")
                + buildDetailRow("When", eventTime, "#FFD230")
                + buildDetailRow("Where", eventVenue + " (" + eventAddress + ")", "#F8FAFC")
                + buildDetailRow("Seats (PAX)", pax + " Shuffler(s)", "#4AB4A4")
                + guestListRow
                + buildDetailRow("Payment Status", escapeHtml(nonEmpty(registration.getPaymentStatus(), "Confirmed")), "#4AB4A4")
                + buildDetailRow("Registration ID", escapeHtml(nonEmpty(registration.getId(), "-")), "#CBD5E1")
                + "</table>"
                + buildQrPassBlock(qrImageUrl, qrToken, "#9440DD");

        return buildThemedEmail(
                "CONFIRMED MEETUP PASS",
                "Holy Meeples, " + recipientName + "! You&rsquo;re In!",
                eventTitle,
                "#9440DD",
                "#4AB4A4",
                body,
                "Show your Digital Check-In QR Pass at the venue entrance."
        );
    }

    public String generateUpcomingEventReminderEmailHtml(Registration registration, ShuffleEvent event, String customNote) {
        String recipientName = escapeHtml(nonEmpty(registration.getParticipantName(), "Fellow Shuffler"));
        String eventTitle = escapeHtml(event != null && event.getTitle() != null ? event.getTitle() : "Upcoming Pune Board Game Meetup");
        String eventVenue = escapeHtml(event != null && event.getVenue() != null ? event.getVenue() : "Pune Venue");
        String eventAddress = escapeHtml(event != null && event.getAddress() != null ? event.getAddress() : "Pune, Maharashtra");
        String eventTime = escapeHtml(event != null && event.getTime() != null ? event.getTime() : "Check app schedule");
        String qrToken = registration.getQrCodeToken() != null ? registration.getQrCodeToken() : ("SS-REG-" + registration.getId());
        String qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=220x220&margin=10&data=" + URLEncoder.encode(qrToken, StandardCharsets.UTF_8);

        String noteCallout = (customNote != null && !customNote.trim().isEmpty())
                ? "<div style='background-color:#10173E;border-left:4px solid #FF751F;border-radius:8px;padding:14px 16px;margin-bottom:18px;font-size:13px;line-height:1.6;color:#F8FAFC;'>"
                  + "<div style='font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:1px;color:#FF751F;margin-bottom:4px;'>Organizer Note</div>"
                  + escapeHtml(customNote).replace("\n", "<br/>") + "</div>"
                : "";

        String body = noteCallout
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:#1A255C;border:1px solid #283677;border-radius:12px;margin-bottom:20px;'>"
                + buildDetailRow("Meetup", eventTitle, "#F8FAFC")
                + buildDetailRow("Venue", eventVenue + " (" + eventAddress + ")", "#4AB4A4")
                + buildDetailRow("Schedule", eventTime, "#FFD230")
                + "</table>"
                + buildQrPassBlock(qrImageUrl, qrToken, "#FF751F");

        return buildThemedEmail(
                "MEETUP REMINDER",
                "Get Ready, " + recipientName + "!",
                eventTitle + " is coming up",
                "#FF751F",
                "#FFD230",
                body,
                "Arrive 10 minutes early for table pairing and rule intros!"
        );
    }

    public String generateEventBroadcastEmailHtml(Registration registration, ShuffleEvent event, String subject, String message, boolean includeQrPass) {
        String recipientName = escapeHtml(nonEmpty(registration.getParticipantName(), "Fellow Shuffler"));
        String eventTitle = escapeHtml(event != null && event.getTitle() != null ? event.getTitle() : "Social Shuffle Meetup");
        String eventVenue = escapeHtml(event != null && event.getVenue() != null ? event.getVenue() : "Pune Venue");
        String eventTime = escapeHtml(event != null && event.getTime() != null ? event.getTime() : "Event Date");
        int pax = registration.getPaxCount() > 0 ? registration.getPaxCount() : 1;
        String qrToken = registration.getQrCodeToken() != null ? registration.getQrCodeToken() : ("SS-REG-" + registration.getId());
        String qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=220x220&margin=10&data=" + URLEncoder.encode(qrToken, StandardCharsets.UTF_8);

        String parsedMessage = escapeHtml(message != null ? message : "")
                .replace("{name}", recipientName)
                .replace("{event_title}", eventTitle)
                .replace("{event_venue}", eventVenue)
                .replace("{event_time}", eventTime)
                .replace("{pax_count}", String.valueOf(pax))
                .replace("{qr_code}", escapeHtml(qrToken))
                .replace("\n", "<br/>");

        String qrBlock = includeQrPass
                ? "<div style='margin-top:20px;'>" + buildQrPassBlock(qrImageUrl, qrToken, "#4AB4A4") + "</div>"
                : "";

        String body = "<p style='margin:0 0 14px 0;font-size:15px;color:#F8FAFC;'>Hello <strong>" + recipientName + "</strong>,</p>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:#1A255C;border:1px solid #283677;border-radius:12px;margin-bottom:16px;'>"
                + buildDetailRow("Event", eventTitle, "#F8FAFC")
                + buildDetailRow("Venue &amp; Time", eventVenue + " &bull; " + eventTime, "#FFD230")
                + "</table>"
                + "<div style='background-color:#10173E;border-left:4px solid #4AB4A4;border-radius:8px;padding:16px;font-size:14px;line-height:1.65;color:#E2E8F0;'>"
                + parsedMessage
                + "</div>"
                + qrBlock;

        return buildThemedEmail(
                "EVENT UPDATE",
                escapeHtml(nonEmpty(subject, eventTitle)),
                eventTitle + " &bull; " + eventVenue,
                "#4AB4A4",
                "#9440DD",
                body,
                "Event broadcast sent to registered attendees of " + eventTitle + "."
        );
    }

    public String generateCommunityAnnouncementEmailHtml(String recipientName, String title, String message) {
        String safeName = escapeHtml(nonEmpty(recipientName, "Shuffler"));
        String safeTitle = escapeHtml(nonEmpty(title, "Community Announcement"));
        String body = "<p style='margin:0 0 14px 0;font-size:15px;color:#F8FAFC;'>Hey <strong>" + safeName + "</strong>,</p>"
                + "<div style='background-color:#10173E;border-left:4px solid #FFD230;border-radius:8px;padding:16px 18px;font-size:14px;line-height:1.7;color:#E2E8F0;'>"
                + escapeHtml(message != null ? message : "").replace("\n", "<br/>")
                + "</div>"
                + "<p style='margin:16px 0 0 0;font-size:12px;color:#94A3B8;'>Stay tuned for upcoming weekend tables across Pune!</p>";

        return buildThemedEmail(
                "COMMUNITY ANNOUNCEMENT",
                safeTitle,
                "Social Shuffle Pune Community Dispatch",
                "#FFD230",
                "#FF751F",
                body,
                "You are receiving this announcement as a member of Social Shuffle Pune."
        );
    }

    // =========================================================================
    // Shared Midnight Indigo Application Theme Builder
    // =========================================================================

    private String buildThemedEmail(String contextBadge,
                                    String headline,
                                    String subheadline,
                                    String accentHex,
                                    String secondaryHex,
                                    String bodyHtml,
                                    String footerNote) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'></head>"
                + "<body style='margin:0;padding:0;background-color:#10173E;font-family:\"Plus Jakarta Sans\",-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,sans-serif;color:#F8FAFC;'>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:#10173E;padding:24px 12px;'>"
                + "<tr><td align='center'>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='max-width:600px;background-color:#151E4D;border:1px solid #283677;border-radius:18px;overflow:hidden;'>"
                // 4-color brand bar mirroring the Social Shuffle 4-piece geometric logo
                + "<tr><td><table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr>"
                + "<td width='25%' height='5' style='background-color:#FF751F;font-size:0;line-height:0;'>&nbsp;</td>"
                + "<td width='25%' height='5' style='background-color:#4AB4A4;font-size:0;line-height:0;'>&nbsp;</td>"
                + "<td width='25%' height='5' style='background-color:#9440DD;font-size:0;line-height:0;'>&nbsp;</td>"
                + "<td width='25%' height='5' style='background-color:#FFD230;font-size:0;line-height:0;'>&nbsp;</td>"
                + "</tr></table></td></tr>"
                // Brand Header Row with mini 4-tile mark & context badge
                + "<tr><td style='padding:18px 24px 14px 24px;border-bottom:1px solid #202B61;background-color:#10173E;'>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr>"
                + "<td valign='middle'>"
                + "<table role='presentation' cellpadding='0' cellspacing='0'><tr>"
                + "<td style='padding-right:10px;' valign='middle'>"
                + "<table role='presentation' cellpadding='1' cellspacing='1' style='background-color:#151E4D;border:1px solid #283677;border-radius:6px;'>"
                + "<tr><td width='9' height='9' style='background-color:#FF751F;border-radius:4px 1px 1px 1px;font-size:0;'>&nbsp;</td>"
                + "<td width='9' height='9' style='background-color:#4AB4A4;border-radius:50%;font-size:0;'>&nbsp;</td></tr>"
                + "<tr><td width='9' height='9' style='background-color:#9440DD;border-radius:50%;font-size:0;'>&nbsp;</td>"
                + "<td width='9' height='9' style='background-color:#FFD230;border-radius:1px 1px 4px 1px;font-size:0;'>&nbsp;</td></tr>"
                + "</table></td>"
                + "<td valign='middle'>"
                + "<div style='font-size:14px;font-weight:900;letter-spacing:0.3px;color:#F8FAFC;'>Social Shuffle <span style='color:" + accentHex + ";'>Pune</span></div>"
                + "<div style='font-size:9px;font-weight:700;letter-spacing:1.4px;text-transform:uppercase;color:#94A3B8;'>BEYOND YOUR &ldquo;BORED&rdquo; GAMES</div>"
                + "</td></tr></table>"
                + "</td>"
                + "<td align='right' valign='middle'>"
                + "<span style='display:inline-block;padding:5px 10px;border-radius:999px;font-size:10px;font-weight:800;letter-spacing:0.9px;text-transform:uppercase;color:" + accentHex + ";background-color:#151E4D;border:1px solid " + accentHex + ";'>"
                + escapeHtml(contextBadge) + "</span>"
                + "</td>"
                + "</tr></table>"
                + "</td></tr>"
                // Context Hero Banner
                + "<tr><td style='padding:24px 24px 20px 24px;background-color:#1A255C;border-bottom:2px solid " + accentHex + ";'>"
                + "<h1 style='margin:0;font-size:22px;line-height:1.25;font-weight:900;color:#F8FAFC;'>" + headline + "</h1>"
                + "<p style='margin:6px 0 0 0;font-size:13px;font-weight:600;color:" + secondaryHex + ";'>" + subheadline + "</p>"
                + "</td></tr>"
                // Main Content Body
                + "<tr><td style='padding:24px;background-color:#151E4D;'>" + bodyHtml + "</td></tr>"
                // Consistent Footer
                + "<tr><td style='padding:16px 24px;background-color:#10173E;border-top:1px solid #202B61;text-align:center;'>"
                + "<div style='font-size:11px;color:#94A3B8;margin-bottom:4px;'>" + footerNote + "</div>"
                + "<div style='font-size:10px;color:#64748B;'>Social Shuffle Pune &bull; Koregaon Park &bull; Baner &bull; Viman Nagar &bull; FC Road &bull; <span style='color:#4AB4A4;'>socialshuffle.pune@gmail.com</span></div>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr></table></body></html>";
    }

    private String buildDetailRow(String label, String valueHtml, String valueColor) {
        return "<tr>"
                + "<td style='padding:10px 14px;border-bottom:1px solid #283677;font-size:12px;font-weight:700;color:#94A3B8;width:36%;vertical-align:top;'>" + label + "</td>"
                + "<td style='padding:10px 14px;border-bottom:1px solid #283677;font-size:13px;font-weight:700;color:" + valueColor + ";text-align:right;vertical-align:top;'>" + valueHtml + "</td>"
                + "</tr>";
    }

    private String buildQrPassBlock(String qrImageUrl, String qrToken, String borderHex) {
        return "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:#10173E;border:2px dashed " + borderHex + ";border-radius:14px;'>"
                + "<tr><td align='center' style='padding:20px 16px;'>"
                + "<div style='font-size:12px;font-weight:800;text-transform:uppercase;letter-spacing:1.2px;color:#F8FAFC;margin-bottom:10px;'>Digital Check-In QR Pass</div>"
                + "<img src='" + qrImageUrl + "' width='180' height='180' alt='Check-in QR Pass' style='display:block;border-radius:10px;background-color:#FFFFFF;padding:8px;margin:0 auto;' />"
                + "<div style='font-family:ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,monospace;font-size:13px;font-weight:800;color:" + borderHex + ";margin-top:10px;letter-spacing:0.8px;'>" + escapeHtml(qrToken) + "</div>"
                + "</td></tr></table>";
    }

    private String resolveFromAddress(Map<String, Object> cfg) {
        String from = str(cfg.get("smtpFrom"));
        if (!from.isEmpty()) return from;
        String user = str(cfg.get("smtpUser"));
        if (!user.isEmpty()) return user;
        String gmail = str(cfg.get("gmailEmail"));
        return !gmail.isEmpty() ? gmail : "socialshuffle.pune@gmail.com";
    }

    private String resolveAdminEmail() {
        Map<String, Object> cfg = appConfigService.getCommunicationsConfig();
        return nonEmpty(str(cfg.get("adminNotificationEmail")), "socialshuffle.pune@gmail.com");
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

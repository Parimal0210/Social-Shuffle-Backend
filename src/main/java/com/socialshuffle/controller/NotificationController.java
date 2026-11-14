package com.socialshuffle.controller;

import com.socialshuffle.model.AuditLog;
import com.socialshuffle.model.NotificationItem;
import com.socialshuffle.model.Participant;
import com.socialshuffle.model.Registration;
import com.socialshuffle.model.ShuffleEvent;
import com.socialshuffle.repository.AuditLogRepository;
import com.socialshuffle.repository.NotificationItemRepository;
import com.socialshuffle.repository.ParticipantRepository;
import com.socialshuffle.repository.RegistrationRepository;
import com.socialshuffle.repository.ShuffleEventRepository;
import com.socialshuffle.service.EmailNotificationService;
import com.socialshuffle.service.EmailNotificationService.EmailDeliveryResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final ShuffleEventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationItemRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final ParticipantRepository participantRepository;
    private final EmailNotificationService emailNotificationService;
    private final Executor mailTaskExecutor;

    public NotificationController(ShuffleEventRepository eventRepository,
                                  RegistrationRepository registrationRepository,
                                  NotificationItemRepository notificationRepository,
                                  AuditLogRepository auditLogRepository,
                                  ParticipantRepository participantRepository,
                                  EmailNotificationService emailNotificationService,
                                  @Qualifier("mailTaskExecutor") Executor mailTaskExecutor) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.participantRepository = participantRepository;
        this.emailNotificationService = emailNotificationService;
        this.mailTaskExecutor = mailTaskExecutor;
    }

    public static class EventBroadcastRequest {
        private String eventId;
        private String subject;
        private String message;
        private String notificationType;
        private String recipientFilter;
        private boolean includeQrPass = true;
        private List<String> channels = Arrays.asList("email", "whatsapp");

        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getNotificationType() { return notificationType; }
        public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
        public String getRecipientFilter() { return recipientFilter; }
        public void setRecipientFilter(String recipientFilter) { this.recipientFilter = recipientFilter; }
        public boolean isIncludeQrPass() { return includeQrPass; }
        public void setIncludeQrPass(boolean includeQrPass) { this.includeQrPass = includeQrPass; }
        public List<String> getChannels() { return channels; }
        public void setChannels(List<String> channels) { this.channels = channels; }
    }

    private record BroadcastItemResult(NotificationItem notificationItem,
                                        Map<String, Object> summary,
                                        boolean emailSent,
                                        boolean whatsappSent,
                                        boolean whatsappEligible) {}

    @PostMapping({"/event-broadcast", "/events/{eventId}/push-notification", "/push-notification/{eventId}"})
    public ResponseEntity<?> sendEventBroadcast(
            @PathVariable(required = false) String eventId,
            @RequestBody EventBroadcastRequest req) {
        long startMs = System.currentTimeMillis();
        String targetEventId = eventId != null ? eventId : req.getEventId();
        if (targetEventId == null || targetEventId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Event ID is required."));
        }
        req.setEventId(targetEventId);

        Optional<ShuffleEvent> eventOpt = eventRepository.findById(targetEventId);
        if (eventOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Event not found with ID: " + targetEventId));
        }
        ShuffleEvent event = eventOpt.get();
        List<Registration> registrations = registrationRepository.findByEventId(targetEventId);

        List<Registration> targets;
        if ("confirmed_only".equalsIgnoreCase(req.getRecipientFilter())) {
            targets = registrations.stream()
                    .filter(r -> !"Cancelled".equalsIgnoreCase(r.getAttendanceStatus()) && "Confirmed".equalsIgnoreCase(r.getPaymentStatus()))
                    .collect(Collectors.toList());
        } else if ("pending_only".equalsIgnoreCase(req.getRecipientFilter())) {
            targets = registrations.stream()
                    .filter(r -> "Pending".equalsIgnoreCase(r.getAttendanceStatus()))
                    .collect(Collectors.toList());
        } else {
            targets = registrations.stream()
                    .filter(r -> !"Cancelled".equalsIgnoreCase(r.getAttendanceStatus()))
                    .collect(Collectors.toList());
        }

        if (targets.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "eventId", event.getId(),
                    "eventTitle", event.getTitle(),
                    "totalRecipients", 0,
                    "emailSentCount", 0,
                    "sentCount", 0,
                    "failedCount", 0,
                    "whatsappSentCount", 0,
                    "whatsappUnavailableCount", 0,
                    "message", "No matching participants found for this event with the selected filter."
            ));
        }

        String subject = req.getSubject() != null && !req.getSubject().trim().isEmpty()
                ? req.getSubject().trim()
                : "Update regarding " + event.getTitle();
        String messageBody = req.getMessage() != null ? req.getMessage().trim() : "";
        String notifType = req.getNotificationType() != null ? req.getNotificationType() : "custom_broadcast";
        String now = Instant.now().toString();

        // Dispatch emails in parallel using bounded mailTaskExecutor pool
        List<CompletableFuture<BroadcastItemResult>> futures = targets.stream()
                .map(reg -> CompletableFuture.supplyAsync(() -> {
                    boolean emailSent = "event_reminder".equalsIgnoreCase(notifType)
                            ? emailNotificationService.sendEventReminderEmail(reg, event, messageBody)
                            : emailNotificationService.sendEventBroadcastEmail(reg, event, subject, messageBody, req.isIncludeQrPass());

                    String rawPhone = reg.getParticipantPhone();
                    String digits = rawPhone != null ? rawPhone.replaceAll("[^0-9]", "") : "";
                    boolean whatsappEligible = digits.length() >= 10;
                    boolean whatsappSent = false;
                    String channelStatus = whatsappEligible
                            ? "Email dispatched (WhatsApp prompt prepared for " + rawPhone + ")"
                            : "WhatsApp unavailable (no valid phone) -> Email dispatched!";

                    NotificationItem item = new NotificationItem(
                            "notif-" + UUID.randomUUID().toString().substring(0, 8),
                            subject,
                            messageBody.isEmpty() ? ("Notification sent for " + event.getTitle()) : messageBody,
                            "event",
                            now,
                            false
                    );
                    item.setCreatedAt(now);
                    item.setRecipientEmail(reg.getParticipantEmail());
                    item.setRecipientPhone(rawPhone);
                    item.setRegistrationId(reg.getId());
                    item.setEventId(event.getId());
                    item.setDeliveryStatus(emailSent ? "Sent" : "Failed");

                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("registrationId", reg.getId());
                    summary.put("name", reg.getParticipantName());
                    summary.put("email", reg.getParticipantEmail());
                    summary.put("phone", rawPhone != null ? rawPhone : "N/A");
                    summary.put("pax", reg.getPaxCount());
                    summary.put("emailSent", emailSent);
                    summary.put("whatsappSent", whatsappSent);
                    summary.put("whatsappAvailable", whatsappEligible);
                    summary.put("status", channelStatus);
                    summary.put("statusSummary", channelStatus);

                    return new BroadcastItemResult(item, summary, emailSent, whatsappSent, whatsappEligible);
                }, mailTaskExecutor))
                .toList();

        List<BroadcastItemResult> results = futures.stream().map(CompletableFuture::join).toList();

        List<NotificationItem> notifItems = new ArrayList<>(results.size());
        List<Map<String, Object>> recipientSummary = new ArrayList<>(results.size());
        int emailSentSuccessCount = 0;
        int whatsappSentSuccessCount = 0;
        int whatsappUnavailableCount = 0;

        for (BroadcastItemResult r : results) {
            notifItems.add(r.notificationItem());
            recipientSummary.add(r.summary());
            if (r.emailSent()) emailSentSuccessCount++;
            if (r.whatsappSent()) whatsappSentSuccessCount++;
            if (!r.whatsappEligible()) whatsappUnavailableCount++;
        }

        notificationRepository.saveAll(notifItems);

        AuditLog audit = new AuditLog(
                "log-" + UUID.randomUUID().toString().substring(0, 8),
                "Admin Organizer",
                "Event Broadcast Notification",
                "Event: " + event.getTitle(),
                "Dispatched to " + targets.size() + " attendees. Emails sent: " + emailSentSuccessCount,
                Instant.now().toString()
        );
        auditLogRepository.save(audit);

        long durationMs = System.currentTimeMillis() - startMs;
        String summaryMessage = "Dispatched email notification to " + emailSentSuccessCount + "/" + targets.size() + " attendees in " + durationMs + "ms.";

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", emailSentSuccessCount > 0);
        resp.put("eventId", event.getId());
        resp.put("eventTitle", event.getTitle());
        resp.put("totalRecipients", targets.size());
        resp.put("emailSentCount", emailSentSuccessCount);
        resp.put("sentCount", emailSentSuccessCount);
        resp.put("failedCount", targets.size() - emailSentSuccessCount);
        resp.put("whatsappSentCount", whatsappSentSuccessCount);
        resp.put("whatsappUnavailableCount", whatsappUnavailableCount);
        resp.put("fallbackEmailOnlyCount", whatsappUnavailableCount);
        resp.put("durationMs", durationMs);
        resp.put("message", summaryMessage);
        resp.put("recipients", recipientSummary);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/send-reminder/{eventId}")
    public ResponseEntity<?> sendEventReminderShortcut(@PathVariable String eventId, @RequestParam(required = false) String note) {
        EventBroadcastRequest req = new EventBroadcastRequest();
        req.setEventId(eventId);
        req.setNotificationType("event_reminder");
        req.setSubject("Reminder: Upcoming Social Shuffle Meetup!");
        req.setMessage(note != null ? note : "Your upcoming board game meetup is happening soon! Check your pass details and arrive 10 minutes early.");
        req.setRecipientFilter("all");
        req.setIncludeQrPass(true);
        return sendEventBroadcast(eventId, req);
    }

    @PostMapping("/registration-confirmation/{registrationId}")
    public ResponseEntity<?> sendRegistrationConfirmation(@PathVariable String registrationId,
                                                          @RequestBody(required = false) Map<String, Object> body) {
        Optional<Registration> regOpt = registrationRepository.findById(registrationId);
        Registration reg = regOpt.orElse(null);

        if (reg == null && body != null && body.get("registration") instanceof Map<?, ?> regMap) {
            reg = new Registration();
            reg.setId(registrationId);
            reg.setEventId(regMap.get("eventId") != null ? String.valueOf(regMap.get("eventId")) : null);
            reg.setParticipantName(regMap.get("participantName") != null ? String.valueOf(regMap.get("participantName")) : "Fellow Shuffler");
            reg.setParticipantEmail(regMap.get("participantEmail") != null ? String.valueOf(regMap.get("participantEmail")) : "");
            reg.setParticipantPhone(regMap.get("participantPhone") != null ? String.valueOf(regMap.get("participantPhone")) : "");
            reg.setPaxCount(regMap.get("paxCount") instanceof Number n ? n.intValue() : 1);
            reg.setPaymentStatus(regMap.get("paymentStatus") != null ? String.valueOf(regMap.get("paymentStatus")) : "Confirmed");
            reg.setQrCodeToken(regMap.get("qrCodeToken") != null ? String.valueOf(regMap.get("qrCodeToken")) : ("SS-REG-" + registrationId));
        }

        if (reg == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Registration not found."));
        }
        if (reg.getParticipantEmail() == null || !reg.getParticipantEmail().contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Valid participant email is required."));
        }

        ShuffleEvent event = reg.getEventId() != null ? eventRepository.findById(reg.getEventId()).orElse(null) : null;
        if (event == null && body != null && body.get("event") instanceof Map<?, ?> evtMap) {
            event = new ShuffleEvent();
            event.setId(reg.getEventId());
            event.setTitle(evtMap.get("title") != null ? String.valueOf(evtMap.get("title")) : "Social Shuffle Meetup");
            event.setVenue(evtMap.get("venue") != null ? String.valueOf(evtMap.get("venue")) : "Pune Venue");
            event.setAddress(evtMap.get("address") != null ? String.valueOf(evtMap.get("address")) : "Pune");
            event.setTime(evtMap.get("time") != null ? String.valueOf(evtMap.get("time")) : "");
        }

        EmailDeliveryResult result = emailNotificationService.sendRegistrationConfirmationSync(reg, event);
        if (result.success()) {
            String now = Instant.now().toString();
            if (regOpt.isPresent()) {
                Registration persisted = regOpt.get();
                persisted.setEmailSent(true);
                persisted.setEmailSentAt(now);
                persisted.setUpdatedAt(now);
                registrationRepository.save(persisted);
            }

            NotificationItem item = new NotificationItem(
                    "notif-" + UUID.randomUUID().toString().substring(0, 8),
                    "Registration Confirmed: " + (event != null ? event.getTitle() : "Social Shuffle"),
                    "Your registration pass and QR token (" + reg.getQrCodeToken() + ") have been sent to your email (" + reg.getParticipantEmail() + ")",
                    "registration",
                    now,
                    false
            );
            item.setCreatedAt(now);
            item.setRecipientEmail(reg.getParticipantEmail());
            item.setRegistrationId(reg.getId());
            item.setEventId(reg.getEventId());
            item.setDeliveryStatus("Sent");
            notificationRepository.save(item);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "registrationId", reg.getId(),
                    "email", reg.getParticipantEmail(),
                    "provider", result.provider(),
                    "message", "Confirmation email dispatched successfully!"
            ));
        }

        return ResponseEntity.status(502).body(Map.of(
                "success", false,
                "registrationId", reg.getId(),
                "email", reg.getParticipantEmail(),
                "message", result.error() != null ? result.error() : "Failed to dispatch confirmation email."
        ));
    }

    @PostMapping("/community-announcement")
    public ResponseEntity<?> sendCommunityAnnouncement(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "Social Shuffle Pune Community Announcement");
        String message = body.getOrDefault("message", "");
        String now = Instant.now().toString();

        List<Participant> participants = participantRepository.findAll().stream()
                .filter(p -> p.getEmail() != null && p.getEmail().contains("@"))
                .toList();

        List<CompletableFuture<NotificationItem>> futures = participants.stream()
                .map(p -> CompletableFuture.supplyAsync(() -> {
                    boolean ok = emailNotificationService.sendCommunityAnnouncementEmail(p.getEmail(), p.getName(), title, message);
                    NotificationItem item = new NotificationItem(
                            "notif-" + UUID.randomUUID().toString().substring(0, 8),
                            title,
                            message,
                            "community",
                            now,
                            false
                    );
                    item.setCreatedAt(now);
                    item.setRecipientEmail(p.getEmail());
                    item.setDeliveryStatus(ok ? "Sent" : "Failed");
                    return ok ? item : null;
                }, mailTaskExecutor))
                .toList();

        List<NotificationItem> sentItems = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        if (!sentItems.isEmpty()) {
            notificationRepository.saveAll(sentItems);
        }

        AuditLog audit = new AuditLog(
                "log-" + UUID.randomUUID().toString().substring(0, 8),
                "Admin Organizer",
                "Community Announcement",
                "All Participants",
                "Sent announcement '" + title + "' to " + sentItems.size() + " participants over email.",
                Instant.now().toString()
        );
        auditLogRepository.save(audit);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "sentCount", sentItems.size(),
                "totalParticipants", participants.size(),
                "message", "Broadcasted community announcement to " + sentItems.size() + " participants."
        ));
    }

    @GetMapping("/event/{eventId}")
    public List<NotificationItem> getNotificationsForEvent(@PathVariable String eventId) {
        return notificationRepository.findByEventIdOrderByTimestampDesc(eventId);
    }

    @GetMapping("/user/{email}")
    public List<NotificationItem> getNotificationsForUser(@PathVariable String email) {
        return notificationRepository.findByRecipientEmailIgnoreCaseOrderByTimestampDesc(email);
    }
}

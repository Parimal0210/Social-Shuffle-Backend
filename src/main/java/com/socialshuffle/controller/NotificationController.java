package com.socialshuffle.controller;

import com.socialshuffle.model.AuditLog;
import com.socialshuffle.model.NotificationItem;
import com.socialshuffle.model.Registration;
import com.socialshuffle.model.ShuffleEvent;
import com.socialshuffle.repository.AuditLogRepository;
import com.socialshuffle.repository.NotificationItemRepository;
import com.socialshuffle.repository.ParticipantRepository;
import com.socialshuffle.repository.RegistrationRepository;
import com.socialshuffle.repository.ShuffleEventRepository;
import com.socialshuffle.service.EmailNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
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

    public NotificationController(ShuffleEventRepository eventRepository,
                                  RegistrationRepository registrationRepository,
                                  NotificationItemRepository notificationRepository,
                                  AuditLogRepository auditLogRepository,
                                  ParticipantRepository participantRepository,
                                  EmailNotificationService emailNotificationService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.participantRepository = participantRepository;
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * DTO for Event Email Broadcast.
     */
    public static class EventBroadcastRequest {
        private String eventId;
        private String subject;
        private String message;
        private String notificationType; // event_reminder, custom_broadcast, pass_reminder
        private String recipientFilter;  // all, confirmed_only, pending_only
        private boolean includeQrPass = true;

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
    }

    /**
     * Admin manually triggers push email notification to all participants of a specific event.
     */
    @PostMapping("/event-broadcast")
    public ResponseEntity<?> sendEventBroadcast(@RequestBody EventBroadcastRequest req) {
        if (req.getEventId() == null || req.getEventId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Event ID is required."));
        }

        Optional<ShuffleEvent> eventOpt = eventRepository.findById(req.getEventId());
        if (eventOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Event not found with ID: " + req.getEventId()));
        }
        ShuffleEvent event = eventOpt.get();

        List<Registration> registrations = registrationRepository.findByEventId(req.getEventId());

        // Apply recipient filter
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
            // All non-cancelled registrations
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
                    "sentCount", 0,
                    "message", "No matching participants found for this event with the selected filter."
            ));
        }

        String subject = req.getSubject() != null && !req.getSubject().trim().isEmpty()
                ? req.getSubject().trim()
                : "Update regarding " + event.getTitle();

        String messageBody = req.getMessage() != null ? req.getMessage().trim() : "";
        String notifType = req.getNotificationType() != null ? req.getNotificationType() : "custom_broadcast";

        List<NotificationItem> notifItems = new ArrayList<>();
        int sentSuccessCount = 0;
        List<Map<String, Object>> recipientSummary = new ArrayList<>();

        for (Registration reg : targets) {
            boolean sent;
            if ("event_reminder".equalsIgnoreCase(notifType)) {
                sent = emailNotificationService.sendEventReminderEmail(reg, event, messageBody);
            } else {
                sent = emailNotificationService.sendEventBroadcastEmail(reg, event, subject, messageBody, req.isIncludeQrPass());
            }

            if (sent) {
                sentSuccessCount++;
            }

            // Create notification record for this recipient
            NotificationItem item = new NotificationItem(
                    "notif-" + UUID.randomUUID().toString().substring(0, 8),
                    subject,
                    messageBody.isEmpty() ? ("Notification sent for " + event.getTitle()) : messageBody,
                    "event",
                    Instant.now().toString(),
                    false
            );
            item.setRecipientEmail(reg.getParticipantEmail());
            item.setEventId(event.getId());
            item.setDeliveryStatus(sent ? "Sent" : "Failed");
            notifItems.add(item);

            recipientSummary.add(Map.of(
                    "name", reg.getParticipantName(),
                    "email", reg.getParticipantEmail(),
                    "pax", reg.getPaxCount(),
                    "status", sent ? "Delivered" : "Failed"
            ));
        }

        notificationRepository.saveAll(notifItems);

        // Audit Log
        AuditLog audit = new AuditLog(
                "log-" + UUID.randomUUID().toString().substring(0, 8),
                "Admin Organizer",
                "Email Notification Push",
                "Event: " + event.getTitle(),
                "Dispatched email notifications to " + sentSuccessCount + " / " + targets.size() + " participants. Type: " + notifType,
                Instant.now().toString()
        );
        auditLogRepository.save(audit);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "eventId", event.getId(),
                "eventTitle", event.getTitle(),
                "totalRecipients", targets.size(),
                "sentCount", sentSuccessCount,
                "message", "Successfully sent email notifications to " + sentSuccessCount + " participants!",
                "recipients", recipientSummary
        ));
    }

    /**
     * Manually triggers upcoming event reminder to all attendees of an event.
     */
    @PostMapping("/send-reminder/{eventId}")
    public ResponseEntity<?> sendEventReminderShortcut(@PathVariable String eventId, @RequestParam(required = false) String note) {
        EventBroadcastRequest req = new EventBroadcastRequest();
        req.setEventId(eventId);
        req.setNotificationType("event_reminder");
        req.setSubject("⏳ Reminder: Upcoming Social Shuffle Meetup!");
        req.setMessage(note != null ? note : "Your upcoming board game meetup is happening soon! Check your pass details and arrive 10 minutes early.");
        req.setRecipientFilter("all");
        req.setIncludeQrPass(true);
        return sendEventBroadcast(req);
    }

    /**
     * Triggers registration confirmation email pass for a specific registration.
     */
    @PostMapping("/registration-confirmation/{registrationId}")
    public ResponseEntity<?> sendRegistrationConfirmation(@PathVariable String registrationId) {
        Optional<Registration> regOpt = registrationRepository.findById(registrationId);
        if (regOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Registration reg = regOpt.get();
        ShuffleEvent event = eventRepository.findById(reg.getEventId()).orElse(null);

        boolean sent = emailNotificationService.sendRegistrationConfirmationEmail(reg, event);
        if (sent) {
            reg.setEmailSent(true);
            reg.setEmailSentAt(Instant.now().toString());
            registrationRepository.save(reg);

            NotificationItem item = new NotificationItem(
                    "notif-" + UUID.randomUUID().toString().substring(0, 8),
                    "Registration Confirmed: " + (event != null ? event.getTitle() : "Social Shuffle"),
                    "Your registration pass and QR token (" + reg.getQrCodeToken() + ") have been sent to your email (" + reg.getParticipantEmail() + ")",
                    "registration",
                    Instant.now().toString(),
                    false
            );
            item.setRecipientEmail(reg.getParticipantEmail());
            item.setEventId(reg.getEventId());
            notificationRepository.save(item);
        }

        return ResponseEntity.ok(Map.of(
                "success", sent,
                "registrationId", reg.getId(),
                "email", reg.getParticipantEmail(),
                "message", sent ? "Confirmation email dispatched successfully!" : "Failed to dispatch email."
        ));
    }

    /**
     * Community announcement broadcast to all participants.
     */
    @PostMapping("/community-announcement")
    public ResponseEntity<?> sendCommunityAnnouncement(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "Social Shuffle Pune Community Announcement");
        String message = body.getOrDefault("message", "");

        List<com.socialshuffle.model.Participant> participants = participantRepository.findAll();
        int sent = 0;
        List<NotificationItem> notifs = new ArrayList<>();

        for (com.socialshuffle.model.Participant p : participants) {
            if (p.getEmail() != null && !p.getEmail().trim().isEmpty()) {
                boolean ok = emailNotificationService.sendCommunityAnnouncementEmail(p.getEmail(), p.getName(), title, message);
                if (ok) sent++;

                NotificationItem item = new NotificationItem(
                        "notif-" + UUID.randomUUID().toString().substring(0, 8),
                        title,
                        message,
                        "community",
                        Instant.now().toString(),
                        false
                );
                item.setRecipientEmail(p.getEmail());
                notifs.add(item);
            }
        }

        notificationRepository.saveAll(notifs);

        AuditLog audit = new AuditLog(
                "log-" + UUID.randomUUID().toString().substring(0, 8),
                "Admin Organizer",
                "Community Announcement",
                "All Participants",
                "Sent announcement '" + title + "' to " + sent + " participants over email.",
                Instant.now().toString()
        );
        auditLogRepository.save(audit);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "sentCount", sent,
                "totalParticipants", participants.size(),
                "message", "Broadcasted community announcement to " + sent + " participants."
        ));
    }

    /**
     * Returns notifications sent for a specific event.
     */
    @GetMapping("/event/{eventId}")
    public List<NotificationItem> getNotificationsForEvent(@PathVariable String eventId) {
        return notificationRepository.findByEventIdOrderByTimestampDesc(eventId);
    }

    /**
     * Returns notifications for a specific attendee email.
     */
    @GetMapping("/user/{email}")
    public List<NotificationItem> getNotificationsForUser(@PathVariable String email) {
        return notificationRepository.findByRecipientEmailOrderByTimestampDesc(email);
    }
}

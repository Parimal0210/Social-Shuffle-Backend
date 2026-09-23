package com.socialshuffle.controller;

import com.socialshuffle.model.AuditLog;
import com.socialshuffle.model.NotificationItem;
import com.socialshuffle.repository.AuditLogRepository;
import com.socialshuffle.repository.NotificationItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuditAndNotificationController {

    private final AuditLogRepository auditLogRepository;
    private final NotificationItemRepository notificationRepository;

    public AuditAndNotificationController(AuditLogRepository auditLogRepository,
                                          NotificationItemRepository notificationRepository) {
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
    }

    // --- Audit Logs ---
    @GetMapping("/audit-logs")
    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    @PostMapping("/audit-logs")
    public AuditLog createAuditLog(@RequestBody AuditLog log) {
        if (log.getId() == null || log.getId().trim().isEmpty()) {
            log.setId("log-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (log.getTimestamp() == null) {
            log.setTimestamp(Instant.now().toString());
        }
        return auditLogRepository.save(log);
    }

    // --- Notifications ---
    @GetMapping("/notifications")
    public List<NotificationItem> getNotifications() {
        return notificationRepository.findAllByOrderByTimestampDesc();
    }

    @PostMapping("/notifications")
    public NotificationItem createNotification(@RequestBody NotificationItem notif) {
        if (notif.getId() == null || notif.getId().trim().isEmpty()) {
            notif.setId("notif-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (notif.getTimestamp() == null) {
            notif.setTimestamp(Instant.now().toString());
        }
        return notificationRepository.save(notif);
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<NotificationItem> markRead(@PathVariable String id) {
        return notificationRepository.findById(id).map(notif -> {
            notif.setRead(true);
            NotificationItem saved = notificationRepository.save(notif);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/notifications/mark-all-read")
    public ResponseEntity<?> markAllRead() {
        List<NotificationItem> unread = notificationRepository.findByReadFalse();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        return ResponseEntity.ok().build();
    }
}

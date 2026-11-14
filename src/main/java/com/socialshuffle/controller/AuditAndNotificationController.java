package com.socialshuffle.controller;

import com.socialshuffle.model.AuditLog;
import com.socialshuffle.model.NotificationItem;
import com.socialshuffle.model.User;
import com.socialshuffle.repository.AuditLogRepository;
import com.socialshuffle.repository.NotificationItemRepository;
import com.socialshuffle.repository.UserRepository;
import com.socialshuffle.security.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuditAndNotificationController {

    private final AuditLogRepository auditLogRepository;
    private final NotificationItemRepository notificationRepository;
    private final UserRepository userRepository;

    public AuditAndNotificationController(AuditLogRepository auditLogRepository,
                                          NotificationItemRepository notificationRepository,
                                          UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    private boolean isAdmin(HttpServletRequest request) {
        return "admin".equalsIgnoreCase(String.valueOf(request.getAttribute(AuthInterceptor.ATTR_ROLE)));
    }

    private User resolveCallerUser(HttpServletRequest request) {
        Object userIdObj = request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        if (userIdObj == null) return null;
        return userRepository.findById(String.valueOf(userIdObj)).orElse(null);
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
    public List<NotificationItem> getNotifications(HttpServletRequest request) {
        if (isAdmin(request)) {
            return notificationRepository.findVisibleOrderByTimestampDesc();
        }
        User caller = resolveCallerUser(request);
        if (caller == null) {
            return notificationRepository.findVisibleForParticipant("", "");
        }
        String email = caller.getEmail() != null ? caller.getEmail().trim() : "";
        String uid = caller.getId() != null ? caller.getId().trim() : "";
        return notificationRepository.findVisibleForParticipant(email, uid);
    }

    @PostMapping("/notifications")
    public NotificationItem createNotification(@RequestBody NotificationItem notif) {
        String now = Instant.now().toString();
        if (notif.getId() == null || notif.getId().trim().isEmpty()) {
            notif.setId("notif-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (notif.getTimestamp() == null) notif.setTimestamp(now);
        if (notif.getCreatedAt() == null) notif.setCreatedAt(now);
        return notificationRepository.save(notif);
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable String id, HttpServletRequest request) {
        Optional<NotificationItem> opt = notificationRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        NotificationItem notif = opt.get();
        if (!isAdmin(request)) {
            User caller = resolveCallerUser(request);
            if (!canAccessNotification(notif, caller)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access denied"));
            }
        }
        notif.setRead(true);
        return ResponseEntity.ok(notificationRepository.save(notif));
    }

    @PostMapping("/notifications/mark-all-read")
    public ResponseEntity<?> markAllRead(HttpServletRequest request) {
        int updated;
        if (isAdmin(request)) {
            updated = notificationRepository.markAllReadFast();
        } else {
            User caller = resolveCallerUser(request);
            if (caller == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Authentication required"));
            }
            updated = notificationRepository.markParticipantReadFast(
                    caller.getEmail() != null ? caller.getEmail() : "",
                    caller.getId() != null ? caller.getId() : ""
            );
        }
        return ResponseEntity.ok(Map.of("success", true, "updatedCount", updated));
    }

    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable String id, HttpServletRequest request) {
        Optional<NotificationItem> opt = notificationRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        if (!isAdmin(request)) {
            User caller = resolveCallerUser(request);
            if (!canAccessNotification(opt.get(), caller)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access denied"));
            }
        }
        notificationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "id", id));
    }

    private boolean canAccessNotification(NotificationItem notif, User caller) {
        if (caller == null) return false;
        if (caller.getId() != null && caller.getId().equals(notif.getUserId())) return true;
        if (caller.getEmail() != null && notif.getRecipientEmail() != null
                && caller.getEmail().equalsIgnoreCase(notif.getRecipientEmail().trim())) {
            return true;
        }
        return (notif.getRecipientEmail() == null || notif.getRecipientEmail().isBlank())
                && ("event".equalsIgnoreCase(notif.getType()) || "community".equalsIgnoreCase(notif.getType()));
    }
}

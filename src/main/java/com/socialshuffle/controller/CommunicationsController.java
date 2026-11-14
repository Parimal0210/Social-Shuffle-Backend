package com.socialshuffle.controller;

import com.socialshuffle.security.AuthInterceptor;
import com.socialshuffle.service.AppConfigService;
import com.socialshuffle.service.EmailNotificationService;
import com.socialshuffle.service.EmailNotificationService.EmailDeliveryResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/communications")
@CrossOrigin(origins = "*")
public class CommunicationsController {

    private final AppConfigService appConfigService;
    private final EmailNotificationService emailNotificationService;

    public CommunicationsController(AppConfigService appConfigService,
                                    EmailNotificationService emailNotificationService) {
        this.appConfigService = appConfigService;
        this.emailNotificationService = emailNotificationService;
    }

    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.putAll(appConfigService.getPublicCommunicationsConfig());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/config")
    public ResponseEntity<?> updateConfig(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String updatedBy = (String) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        appConfigService.saveCommunicationsConfig(body, updatedBy);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "Communications configuration saved to database.");
        resp.putAll(appConfigService.getPublicCommunicationsConfig());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/set-gmail-token")
    public ResponseEntity<?> setGmailToken(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String accessToken = body.get("accessToken") != null ? String.valueOf(body.get("accessToken")).trim() : "";
        if (accessToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "accessToken is required"));
        }
        String email = body.get("email") != null ? String.valueOf(body.get("email")).trim() : "";
        long expiresIn = body.get("expiresIn") instanceof Number ? ((Number) body.get("expiresIn")).longValue() : 3500L;

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("gmailConnected", true);
        updates.put("gmailEmail", email);
        updates.put("gmailAccessToken", accessToken);
        updates.put("gmailExpiresAt", System.currentTimeMillis() + (expiresIn * 1000L));

        String updatedBy = (String) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        appConfigService.saveCommunicationsConfig(updates, updatedBy);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "gmailConnected", true,
                "gmailEmail", email,
                "message", "Gmail OAuth token linked successfully."
        ));
    }

    @GetMapping("/gmail-status")
    public ResponseEntity<?> getGmailStatus() {
        Map<String, Object> cfg = appConfigService.getCommunicationsConfig();
        long expiresAt = cfg.get("gmailExpiresAt") instanceof Number ? ((Number) cfg.get("gmailExpiresAt")).longValue() : 0L;
        boolean connected = Boolean.TRUE.equals(cfg.get("gmailConnected")) && (expiresAt == 0L || expiresAt > System.currentTimeMillis());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "gmailConnected", connected,
                "gmailEmail", cfg.getOrDefault("gmailEmail", "")
        ));
    }

    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestBody Map<String, Object> body) {
        String toEmail = body.get("toEmail") != null ? String.valueOf(body.get("toEmail")).trim() : "";
        if (toEmail.isEmpty() || !toEmail.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Valid toEmail is required."));
        }
        String subject = body.get("subject") != null && !String.valueOf(body.get("subject")).trim().isEmpty()
                ? String.valueOf(body.get("subject")).trim()
                : "Social Shuffle Pune - Test Email";
        String message = body.get("message") != null && !String.valueOf(body.get("message")).trim().isEmpty()
                ? String.valueOf(body.get("message")).trim()
                : "SMTP / Email transport test from Social Shuffle Java STS backend succeeded!";

        String html = "<div style='font-family:sans-serif;max-width:560px;margin:20px auto;background:#0f172a;color:#e2e8f0;padding:24px;border-radius:12px;border:1px solid #334155'>"
                + "<h2 style='color:#35C8A8;margin-top:0'>" + subject + "</h2>"
                + "<p>" + message + "</p>"
                + "</div>";
        EmailDeliveryResult res = emailNotificationService.dispatchEmail(toEmail, subject, html, message);
        if (!res.success()) {
            return ResponseEntity.status(502).body(Map.of(
                    "success", false,
                    "provider", res.provider(),
                    "error", res.error() != null ? res.error() : "Failed to deliver test email"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "provider", res.provider(),
                "messageId", res.messageId() != null ? res.messageId() : "",
                "message", "Test email dispatched via " + res.provider()
        ));
    }

    @PostMapping("/test-whatsapp")
    public ResponseEntity<?> testWhatsapp(@RequestBody Map<String, Object> body) {
        String toPhone = body.get("toPhone") != null ? String.valueOf(body.get("toPhone")).trim() : "";
        String digits = toPhone.replaceAll("[^0-9]", "");
        if (digits.length() < 10) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Valid 10+ digit phone number required"));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "toPhone", toPhone,
                "message", "WhatsApp format validated and ready for community dispatch."
        ));
    }
}

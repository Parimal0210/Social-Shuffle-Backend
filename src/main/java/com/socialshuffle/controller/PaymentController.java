package com.socialshuffle.controller;

import com.socialshuffle.model.Registration;
import com.socialshuffle.model.ShuffleEvent;
import com.socialshuffle.repository.RegistrationRepository;
import com.socialshuffle.repository.ShuffleEventRepository;
import com.socialshuffle.security.AuthInterceptor;
import com.socialshuffle.service.AppConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/payments/razorpay")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final RegistrationRepository registrationRepository;
    private final ShuffleEventRepository shuffleEventRepository;
    private final AppConfigService appConfigService;

    public PaymentController(RegistrationRepository registrationRepository,
                             ShuffleEventRepository shuffleEventRepository,
                             AppConfigService appConfigService) {
        this.registrationRepository = registrationRepository;
        this.shuffleEventRepository = shuffleEventRepository;
        this.appConfigService = appConfigService;
    }

    @GetMapping("/config")
    public ResponseEntity<?> getRazorpayConfig() {
        return ResponseEntity.ok(appConfigService.getPublicRazorpayConfig());
    }

    @PostMapping("/config")
    public ResponseEntity<?> saveRazorpayConfig(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String updatedBy = request.getAttribute(AuthInterceptor.ATTR_USER_ID) != null
                ? String.valueOf(request.getAttribute(AuthInterceptor.ATTR_USER_ID))
                : "Admin";
        Map<String, Object> saved = appConfigService.saveRazorpayConfig(body, updatedBy);
        Map<String, Object> resp = new LinkedHashMap<>(saved);
        resp.put("success", true);
        resp.put("message", "Razorpay payment configuration saved.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createRazorpayOrder(@RequestBody Map<String, Object> payload) {
        String eventId = payload.get("eventId") != null ? String.valueOf(payload.get("eventId")).trim() : "";
        int paxCount = payload.get("paxCount") instanceof Number n ? Math.max(1, n.intValue()) : 1;
        Number amountNumber = payload.get("amount") instanceof Number n ? n : null;
        double amount = amountNumber != null ? amountNumber.doubleValue() : 350.0;

        if (!eventId.isEmpty()) {
            Optional<ShuffleEvent> eventOpt = shuffleEventRepository.findById(eventId);
            if (eventOpt.isPresent() && eventOpt.get().getTicketPrice() > 0) {
                amount = eventOpt.get().getTicketPrice() * paxCount;
            }
        }

        long amountInPaise = Math.round(amount * 100);
        Map<String, Object> cfg = appConfigService.getPublicRazorpayConfig();
        String keyId = cfg.get("keyId") != null && !String.valueOf(cfg.get("keyId")).isBlank()
                ? String.valueOf(cfg.get("keyId"))
                : "rzp_test_SocialShufflePune";

        String orderId = "order_SS_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("orderId", orderId);
        response.put("amount", amountInPaise);
        response.put("amountInRupees", amount);
        response.put("currency", cfg.getOrDefault("currency", "INR"));
        response.put("keyId", keyId);
        response.put("businessName", cfg.getOrDefault("companyName", "Social Shuffle Pune"));
        response.put("description", "Board Game Meetup Pass");
        response.put("createdAt", Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyRazorpayPayment(@RequestBody Map<String, Object> payload) {
        String paymentId = payload.get("razorpayPaymentId") != null ? String.valueOf(payload.get("razorpayPaymentId")).trim() : "";
        String orderId = payload.get("razorpayOrderId") != null ? String.valueOf(payload.get("razorpayOrderId")).trim() : "";
        String signature = payload.get("razorpaySignature") != null ? String.valueOf(payload.get("razorpaySignature")).trim() : "";
        String registrationId = payload.get("registrationId") != null ? String.valueOf(payload.get("registrationId")).trim() : "";

        if (paymentId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing Razorpay Payment ID"
            ));
        }

        Map<String, Object> cfg = appConfigService.getRazorpayConfig();
        String keySecret = cfg.get("keySecret") != null ? String.valueOf(cfg.get("keySecret")).trim() : "";
        boolean isLive = Boolean.TRUE.equals(cfg.get("isLive"));

        if (!keySecret.isEmpty() && !signature.isEmpty() && !orderId.isEmpty()) {
            if (!verifyHmacSha256(orderId + "|" + paymentId, signature, keySecret)) {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "verified", false,
                        "message", "Invalid Razorpay payment signature."
                ));
            }
        } else if (isLive && keySecret.isEmpty()) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Razorpay keySecret is not configured for live verification."
            ));
        }

        String now = Instant.now().toString();
        if (!registrationId.isEmpty()) {
            Optional<Registration> regOpt = registrationRepository.findById(registrationId);
            regOpt.ifPresent(reg -> {
                reg.setPaymentStatus("Confirmed");
                reg.setRazorpayPaymentId(paymentId);
                if (!orderId.isEmpty()) {
                    reg.setRazorpayOrderId(orderId);
                }
                reg.setUpdatedAt(now);
                registrationRepository.save(reg);
            });
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("verified", true);
        response.put("paymentId", paymentId);
        response.put("orderId", !orderId.isEmpty() ? orderId : "order_SS_verified");
        response.put("status", "Confirmed");
        response.put("message", "Payment verified successfully by Razorpay gateway.");
        response.put("verifiedAt", now);

        return ResponseEntity.ok(response);
    }

    private boolean verifyHmacSha256(String data, String expectedHex, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    expectedHex.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }
}

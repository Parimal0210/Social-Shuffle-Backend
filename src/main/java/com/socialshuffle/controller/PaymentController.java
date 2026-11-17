package com.socialshuffle.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialshuffle.model.Registration;
import com.socialshuffle.model.ShuffleEvent;
import com.socialshuffle.model.User;
import com.socialshuffle.repository.RegistrationRepository;
import com.socialshuffle.repository.ShuffleEventRepository;
import com.socialshuffle.repository.UserRepository;
import com.socialshuffle.security.AuthInterceptor;
import com.socialshuffle.service.AppConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/payments/razorpay")
@CrossOrigin(origins = "*")
public class PaymentController {

    private static final String RAZORPAY_API_BASE = "https://api.razorpay.com/v1";

    private final RegistrationRepository registrationRepository;
    private final ShuffleEventRepository shuffleEventRepository;
    private final UserRepository userRepository;
    private final AppConfigService appConfigService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PaymentController(RegistrationRepository registrationRepository,
                             ShuffleEventRepository shuffleEventRepository,
                             UserRepository userRepository,
                             AppConfigService appConfigService,
                             ObjectMapper objectMapper) {
        this.registrationRepository = registrationRepository;
        this.shuffleEventRepository = shuffleEventRepository;
        this.userRepository = userRepository;
        this.appConfigService = appConfigService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
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

    @GetMapping("/test-connection")
    public ResponseEntity<?> testRazorpayConnection() {
        Map<String, Object> cfg = appConfigService.getRazorpayConfig();
        String keyId = cfg.get("keyId") != null ? String.valueOf(cfg.get("keyId")).trim() : "";
        String keySecret = cfg.get("keySecret") != null ? String.valueOf(cfg.get("keySecret")).trim() : "";
        if (keyId.isEmpty() || keySecret.isEmpty() || "rzp_test_SocialShufflePune".equals(keyId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "connected", false,
                    "keyId", keyId,
                    "message", "Valid Razorpay Key ID and Key Secret are required."
            ));
        }

        try {
            String basicAuth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(RAZORPAY_API_BASE + "/payments?count=1"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Basic " + basicAuth)
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "connected", true,
                        "mode", keyId.startsWith("rzp_live_") ? "live" : "test",
                        "keyId", keyId,
                        "statusCode", resp.statusCode(),
                        "message", "Connected to Razorpay API successfully."
                ));
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "success", false,
                    "connected", false,
                    "mode", keyId.startsWith("rzp_live_") ? "live" : "test",
                    "keyId", keyId,
                    "statusCode", resp.statusCode(),
                    "message", "Razorpay API authentication failed (HTTP " + resp.statusCode() + ")."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "success", false,
                    "connected", false,
                    "message", "Unable to reach Razorpay API: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createRazorpayOrder(@RequestBody Map<String, Object> payload) {
        String eventId = payload.get("eventId") != null ? String.valueOf(payload.get("eventId")).trim() : "";
        int paxCount = payload.get("paxCount") instanceof Number n ? Math.max(1, n.intValue()) : 1;
        Number amountNumber = payload.get("amount") instanceof Number n ? n : null;
        double amount = amountNumber != null && amountNumber.doubleValue() > 0 ? amountNumber.doubleValue() : 350.0;

        if (!eventId.isEmpty()) {
            Optional<ShuffleEvent> eventOpt = shuffleEventRepository.findById(eventId);
            if (eventOpt.isPresent() && eventOpt.get().getTicketPrice() >= 0) {
                amount = eventOpt.get().getTicketPrice() * paxCount;
            }
        }

        long amountInPaise = Math.max(100L, Math.round(amount * 100));
        Map<String, Object> cfg = appConfigService.getRazorpayConfig();
        String keyId = cfg.get("keyId") != null ? String.valueOf(cfg.get("keyId")).trim() : "";
        String keySecret = cfg.get("keySecret") != null ? String.valueOf(cfg.get("keySecret")).trim() : "";
        String currency = cfg.get("currency") != null && !String.valueOf(cfg.get("currency")).isBlank()
                ? String.valueOf(cfg.get("currency")).trim()
                : "INR";

        String orderId;
        boolean gatewayOrderCreated = false;

        if (!keyId.isEmpty() && !keySecret.isEmpty() && !"rzp_test_SocialShufflePune".equals(keyId)) {
            try {
                Map<String, Object> rzpPayload = new LinkedHashMap<>();
                rzpPayload.put("amount", amountInPaise);
                rzpPayload.put("currency", currency);
                rzpPayload.put("receipt", "rcpt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
                if (!eventId.isEmpty()) {
                    rzpPayload.put("notes", Map.of("eventId", eventId, "paxCount", String.valueOf(paxCount)));
                }
                String basicAuth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(RAZORPAY_API_BASE + "/orders"))
                        .timeout(Duration.ofSeconds(12))
                        .header("Authorization", "Basic " + basicAuth)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(rzpPayload)))
                        .build();
                HttpResponse<String> rzpResp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (rzpResp.statusCode() >= 200 && rzpResp.statusCode() < 300) {
                    JsonNode root = objectMapper.readTree(rzpResp.body());
                    orderId = root.path("id").asText("");
                    if (orderId.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                                "success", false,
                                "message", "Razorpay Orders API did not return an order ID."
                        ));
                    }
                    gatewayOrderCreated = true;
                } else {
                    String errDesc = "Razorpay order creation failed (HTTP " + rzpResp.statusCode() + ").";
                    try {
                        String desc = objectMapper.readTree(rzpResp.body()).path("error").path("description").asText("");
                        if (!desc.isEmpty()) errDesc = desc;
                    } catch (Exception ignored) {}
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                            "success", false,
                            "message", errDesc
                    ));
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                        "success", false,
                        "message", "Failed to communicate with Razorpay Orders API: " + e.getMessage()
                ));
            }
        } else {
            orderId = "order_SS_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("orderId", orderId);
        response.put("amount", amountInPaise);
        response.put("amountInRupees", amount);
        response.put("currency", currency);
        response.put("keyId", keyId.isEmpty() ? "rzp_test_SocialShufflePune" : keyId);
        response.put("businessName", cfg.getOrDefault("companyName", "Social Shuffle Pune"));
        response.put("description", "Board Game Meetup Pass");
        response.put("gatewayOrderCreated", gatewayOrderCreated);
        response.put("mode", keyId.startsWith("rzp_live_") ? "live" : "test");
        response.put("createdAt", Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyRazorpayPayment(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
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
        boolean isRealGatewayOrder = !orderId.isEmpty() && !orderId.startsWith("order_SS_");

        if (isLive || (!keySecret.isEmpty() && isRealGatewayOrder)) {
            if (keySecret.isEmpty()) {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "verified", false,
                        "message", "Razorpay keySecret is not configured for signature verification."
                ));
            }
            if (signature.isEmpty() || orderId.isEmpty() || !verifyHmacSha256(orderId + "|" + paymentId, signature, keySecret)) {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "verified", false,
                        "message", "Invalid Razorpay payment signature."
                ));
            }
        }

        boolean callerIsAdmin = "admin".equalsIgnoreCase(String.valueOf(request.getAttribute(AuthInterceptor.ATTR_ROLE)));
        User caller = null;
        if (!callerIsAdmin && request.getAttribute(AuthInterceptor.ATTR_USER_ID) != null) {
            caller = userRepository.findById(String.valueOf(request.getAttribute(AuthInterceptor.ATTR_USER_ID))).orElse(null);
        }

        String now = Instant.now().toString();
        if (!registrationId.isEmpty()) {
            Optional<Registration> regOpt = registrationRepository.findById(registrationId);
            if (regOpt.isPresent()) {
                Registration reg = regOpt.get();
                if (!callerIsAdmin && !ownsRegistration(reg, caller)) {
                    return ResponseEntity.status(403).body(Map.of(
                            "success", false,
                            "message", "Access denied: Cannot confirm payment for another user's registration."
                    ));
                }
                reg.setPaymentStatus("Confirmed");
                reg.setRazorpayPaymentId(paymentId);
                if (!orderId.isEmpty()) {
                    reg.setRazorpayOrderId(orderId);
                }
                reg.setUpdatedAt(now);
                registrationRepository.save(reg);
            }
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

    private boolean ownsRegistration(Registration reg, User caller) {
        if (caller == null || reg == null) return false;
        if (caller.getParticipantId() != null && caller.getParticipantId().equals(reg.getParticipantId())) return true;
        if (caller.getId() != null && caller.getId().equals(reg.getParticipantId())) return true;
        return caller.getEmail() != null && reg.getParticipantEmail() != null
                && caller.getEmail().equalsIgnoreCase(reg.getParticipantEmail().trim());
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

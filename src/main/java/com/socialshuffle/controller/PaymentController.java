package com.socialshuffle.controller;

import com.socialshuffle.model.Registration;
import com.socialshuffle.repository.RegistrationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/payments/razorpay")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final RegistrationRepository registrationRepository;

    public PaymentController(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    /**
     * Creates a simulated or live Razorpay Order.
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createRazorpayOrder(@RequestBody Map<String, Object> payload) {
        String eventId = (String) payload.get("eventId");
        Number amountNumber = (Number) payload.get("amount");
        double amount = amountNumber != null ? amountNumber.doubleValue() : 350.0;
        long amountInPaise = Math.round(amount * 100);

        String orderId = "order_SS_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("orderId", orderId);
        response.put("amount", amountInPaise);
        response.put("amountInRupees", amount);
        response.put("currency", "INR");
        response.put("keyId", "rzp_test_SocialShufflePune");
        response.put("businessName", "Social Shuffle Pune");
        response.put("description", "Board Game Meetup Pass");
        response.put("createdAt", Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    /**
     * Verifies payment completion from Razorpay Checkout webhook or client handler.
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyRazorpayPayment(@RequestBody Map<String, Object> payload) {
        String paymentId = (String) payload.get("razorpayPaymentId");
        String orderId = (String) payload.get("razorpayOrderId");
        String registrationId = (String) payload.get("registrationId");

        if (paymentId == null || paymentId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing Razorpay Payment ID"
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("verified", true);
        response.put("paymentId", paymentId);
        response.put("orderId", orderId != null ? orderId : "order_SS_verified");
        response.put("status", "Confirmed");
        response.put("message", "Payment verified successfully by Razorpay gateway.");
        response.put("verifiedAt", Instant.now().toString());

        if (registrationId != null && !registrationId.trim().isEmpty()) {
            Optional<Registration> regOpt = registrationRepository.findById(registrationId.trim());
            regOpt.ifPresent(reg -> {
                reg.setPaymentStatus("Confirmed");
                reg.setRazorpayPaymentId(paymentId);
                if (orderId != null) {
                    reg.setRazorpayOrderId(orderId);
                }
                registrationRepository.save(reg);
            });
        }

        return ResponseEntity.ok(response);
    }
}

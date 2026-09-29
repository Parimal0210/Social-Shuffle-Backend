package com.socialshuffle.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class SecurityTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_VALIDITY_MS = 24 * 60 * 60 * 1000L; // 24 hours

    @Value("${app.security.jwt-secret:SocialShuffleSuperSecureKeyForProductionPuneCommunity2026!}")
    private String jwtSecret;

    /**
     * Issues an HMAC-SHA256 signed bearer token:
     * payload = userId:role:issuedAt:expiresAt
     * token = base64(payload) + "." + base64(signature)
     */
    public String generateToken(String userId, String role) {
        long now = System.currentTimeMillis();
        long expiry = now + TOKEN_VALIDITY_MS;
        String payload = userId + ":" + role + ":" + now + ":" + expiry;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return encodedPayload + "." + signature;
    }

    /**
     * Validates token signature and expiration.
     */
    public boolean validateToken(String token) {
        if (token == null || !token.contains(".")) {
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return false;
        }
        String encodedPayload = parts[0];
        String signature = parts[1];

        // Verify HMAC signature
        String expectedSignature = sign(encodedPayload);
        if (!expectedSignature.equals(signature)) {
            return false;
        }

        // Verify expiry
        try {
            String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            String[] fields = payload.split(":");
            if (fields.length < 4) {
                return false;
            }
            long expiry = Long.parseLong(fields[3]);
            return System.currentTimeMillis() < expiry;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts user role from validated token.
     */
    public String extractRole(String token) {
        try {
            if (!validateToken(token)) return null;
            String encodedPayload = token.split("\\.")[0];
            String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            String[] fields = payload.split(":");
            return fields.length >= 2 ? fields[1] : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts userId from validated token.
     */
    public String extractUserId(String token) {
        try {
            if (!validateToken(token)) return null;
            String encodedPayload = token.split("\\.")[0];
            String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            String[] fields = payload.split(":");
            return fields.length >= 1 ? fields[0] : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error computing token signature", e);
        }
    }
}

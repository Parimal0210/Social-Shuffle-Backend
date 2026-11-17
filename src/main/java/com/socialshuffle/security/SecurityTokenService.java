package com.socialshuffle.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SecurityTokenService {

    private static final Logger log = LoggerFactory.getLogger(SecurityTokenService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String DEFAULT_DEV_SECRET = "SocialShuffleSuperSecureHMAC256KeyPuneCommunityBoardGames2026!";

    @Value("${app.security.token-validity-ms:2592000000}")
    private long tokenValidityMs;

    @Value("${app.security.jwt-secret:" + DEFAULT_DEV_SECRET + "}")
    private String jwtSecret;

    // Revoked token IDs mapped to their expiration timestamp for cleanup
    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    @PostConstruct
    public void validateSecretConfiguration() {
        if (jwtSecret == null || jwtSecret.trim().length() < 32 || DEFAULT_DEV_SECRET.equals(jwtSecret)) {
            log.warn("[SECURITY] Using default development JWT signing key. Set APP_JWT_SECRET (>=32 chars) in production!");
        }
    }

    public String generateToken(String userId, String role) {
        long now = System.currentTimeMillis();
        long expiry = now + tokenValidityMs;
        String jti = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String payload = userId + ":" + role + ":" + jti + ":" + now + ":" + expiry;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return encodedPayload + "." + signature;
    }

    public boolean validateToken(String token) {
        String[] fields = parseValidatedFields(token);
        return fields != null;
    }

    public String extractRole(String token) {
        String[] fields = parseValidatedFields(token);
        return (fields != null && fields.length >= 2) ? fields[1] : null;
    }

    public String extractUserId(String token) {
        String[] fields = parseValidatedFields(token);
        return (fields != null && fields.length >= 1) ? fields[0] : null;
    }

    public void revokeToken(String token) {
        if (token == null || !token.contains(".")) {
            return;
        }
        String[] fields = parseValidatedFields(token);
        if (fields != null) {
            if (fields.length >= 5) {
                long expiry = Long.parseLong(fields[4]);
                revokedTokens.put(fields[2], expiry);
            } else if (fields.length == 4) {
                long expiry = Long.parseLong(fields[3]);
                revokedTokens.put(token, expiry);
            }
        }
        long now = System.currentTimeMillis();
        if (revokedTokens.size() > 250) {
            revokedTokens.entrySet().removeIf(e -> e.getValue() < now);
        }
    }

    private String[] parseValidatedFields(String token) {
        if (token == null || !token.contains(".")) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return null;
        }
        String encodedPayload = parts[0];
        String signature = parts[1];

        String expectedSignature = sign(encodedPayload);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            return null;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            String[] fields = payload.split(":");
            long now = System.currentTimeMillis();
            if (fields.length >= 5) {
                long expiry = Long.parseLong(fields[4]);
                if (now >= expiry) return null;
                if (revokedTokens.containsKey(fields[2])) return null;
                return fields;
            } else if (fields.length == 4) {
                long expiry = Long.parseLong(fields[3]);
                if (now >= expiry) return null;
                if (revokedTokens.containsKey(token)) return null;
                return fields;
            }
            return null;
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

package com.socialshuffle.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

@Component
public class PasswordSecurityUtil {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Hashes password using PBKDF2 with HMAC-SHA256 and cryptographic salt.
     * Returns format: pbkdf2:<salt_base64>:<hash_base64>
     */
    public String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        return "pbkdf2:" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies password using constant-time comparison to prevent timing attacks.
     * Supports both PBKDF2 hashed passwords and legacy fallback.
     */
    public boolean verifyPassword(String candidatePassword, String storedHash) {
        if (candidatePassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        // PBKDF2 Format
        if (storedHash.startsWith("pbkdf2:")) {
            String[] parts = storedHash.split(":");
            if (parts.length != 3) {
                return false;
            }
            try {
                byte[] salt = Base64.getDecoder().decode(parts[1]);
                byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
                byte[] candidateHash = pbkdf2(candidatePassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
                return slowEquals(expectedHash, candidateHash);
            } catch (Exception e) {
                return false;
            }
        }

        // Backward compatibility for initial seeds / demo accounts
        return slowEquals(storedHash.getBytes(), candidatePassword.getBytes());
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Constant-time byte comparison to thwart side-channel timing attacks.
     */
    private boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return false;
        }
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}

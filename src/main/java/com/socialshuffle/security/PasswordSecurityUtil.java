package com.socialshuffle.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class PasswordSecurityUtil {

    private static final int SHA256_ITERATIONS = 65536;
    private static final int SHA256_KEY_LENGTH = 256;
    private static final int SHA512_ITERATIONS = 100000;
    private static final int SHA512_KEY_LENGTH = 512;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Hashes password using PBKDF2WithHmacSHA512 matching unified user_credentials format:
     * pbkdf2_sha512$100000$<salt_hex>$<hash_hex>
     */
    public String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        byte[] saltBytes = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(saltBytes);
        String saltHex = HexFormat.of().formatHex(saltBytes);

        byte[] derived = pbkdf2("PBKDF2WithHmacSHA512", password.toCharArray(), saltHex.getBytes(StandardCharsets.UTF_8), SHA512_ITERATIONS, SHA512_KEY_LENGTH);
        return "pbkdf2_sha512$" + SHA512_ITERATIONS + "$" + saltHex + "$" + HexFormat.of().formatHex(derived);
    }

    /**
     * Verifies password using constant-time comparison across:
     * 1) pbkdf2_sha512$<iterations>$<salt_utf8>$<hash_hex>
     * 2) pbkdf2:<salt_b64>:<hash_b64> (legacy Java SHA-256)
     * 3) legacy plaintext fallback
     */
    public boolean verifyPassword(String candidatePassword, String storedHash) {
        if (candidatePassword == null || candidatePassword.isEmpty() || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        if (storedHash.startsWith("pbkdf2_sha512$")) {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            try {
                int iterations = Integer.parseInt(parts[1]);
                String salt = parts[2];
                byte[] expectedHash = HexFormat.of().parseHex(parts[3]);
                byte[] candidateHash = pbkdf2("PBKDF2WithHmacSHA512", candidatePassword.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), iterations, expectedHash.length * 8);
                return MessageDigest.isEqual(expectedHash, candidateHash);
            } catch (Exception e) {
                return false;
            }
        }

        if (storedHash.startsWith("pbkdf2:")) {
            String[] parts = storedHash.split(":");
            if (parts.length != 3) {
                return false;
            }
            try {
                byte[] salt = Base64.getDecoder().decode(parts[1]);
                byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
                byte[] candidateHash = pbkdf2("PBKDF2WithHmacSHA256", candidatePassword.toCharArray(), salt, SHA256_ITERATIONS, SHA256_KEY_LENGTH);
                return MessageDigest.isEqual(expectedHash, candidateHash);
            } catch (Exception e) {
                return false;
            }
        }

        return MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8), candidatePassword.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isHashed(String value) {
        return value != null && (value.startsWith("pbkdf2_sha512$") || value.startsWith("pbkdf2:"));
    }

    private byte[] pbkdf2(String algorithm, char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password with " + algorithm, e);
        }
    }
}

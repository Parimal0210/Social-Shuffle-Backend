package com.socialshuffle.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 10 * 60 * 1000L; // 10 minutes

    private static class AttemptRecord {
        int count;
        long lastAttemptTime;
        long lockedUntil;

        AttemptRecord(int count, long lastAttemptTime) {
            this.count = count;
            this.lastAttemptTime = lastAttemptTime;
            this.lockedUntil = 0;
        }
    }

    private final Map<String, AttemptRecord> attemptsMap = new ConcurrentHashMap<>();

    /**
     * Checks if identifier (IP or username/email) is currently blocked due to repeated failures.
     */
    public boolean isBlocked(String key) {
        if (key == null) return false;
        AttemptRecord record = attemptsMap.get(key.toLowerCase());
        if (record == null) return false;

        long now = System.currentTimeMillis();
        if (record.lockedUntil > now) {
            return true;
        }
        // If lockout expired, reset
        if (record.lockedUntil > 0 && record.lockedUntil <= now) {
            attemptsMap.remove(key.toLowerCase());
            return false;
        }
        return false;
    }

    /**
     * Records a failed authentication attempt.
     */
    public void recordFailedAttempt(String key) {
        if (key == null) return;
        String normalizedKey = key.toLowerCase();
        long now = System.currentTimeMillis();

        attemptsMap.compute(normalizedKey, (k, existing) -> {
            if (existing == null || (now - existing.lastAttemptTime > LOCKOUT_DURATION_MS && existing.lockedUntil <= now)) {
                return new AttemptRecord(1, now);
            }
            existing.count++;
            existing.lastAttemptTime = now;
            if (existing.count >= MAX_FAILED_ATTEMPTS) {
                existing.lockedUntil = now + LOCKOUT_DURATION_MS;
            }
            return existing;
        });
    }

    /**
     * Clears failed attempts upon successful authentication.
     */
    public void recordSuccessfulAttempt(String key) {
        if (key != null) {
            attemptsMap.remove(key.toLowerCase());
        }
    }

    /**
     * Remaining seconds of lockout for client messaging.
     */
    public long getRemainingLockoutSeconds(String key) {
        if (key == null) return 0;
        AttemptRecord record = attemptsMap.get(key.toLowerCase());
        if (record == null || record.lockedUntil <= System.currentTimeMillis()) {
            return 0;
        }
        return (record.lockedUntil - System.currentTimeMillis()) / 1000;
    }
}

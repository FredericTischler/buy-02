package com.ecommerce.user.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_SECONDS = 300; // 5 minutes

    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        AttemptInfo info = attempts.get(key);
        if (info == null) return false;

        if (info.count >= MAX_ATTEMPTS) {
            if (Instant.now().getEpochSecond() - info.lastAttempt < BLOCK_DURATION_SECONDS) {
                return true;
            }
            // Block duration expired, reset
            attempts.remove(key);
            return false;
        }
        return false;
    }

    public void recordFailedAttempt(String key) {
        attempts.compute(key, (k, info) -> {
            if (info == null) {
                return new AttemptInfo(1, Instant.now().getEpochSecond());
            }
            // If block duration expired, reset counter
            if (Instant.now().getEpochSecond() - info.lastAttempt >= BLOCK_DURATION_SECONDS) {
                return new AttemptInfo(1, Instant.now().getEpochSecond());
            }
            return new AttemptInfo(info.count + 1, Instant.now().getEpochSecond());
        });
    }

    public void resetAttempts(String key) {
        attempts.remove(key);
    }

    public long getRemainingBlockSeconds(String key) {
        AttemptInfo info = attempts.get(key);
        if (info == null) return 0;
        long elapsed = Instant.now().getEpochSecond() - info.lastAttempt;
        return Math.max(0, BLOCK_DURATION_SECONDS - elapsed);
    }

    private static class AttemptInfo {
        final int count;
        final long lastAttempt;

        AttemptInfo(int count, long lastAttempt) {
            this.count = count;
            this.lastAttempt = lastAttempt;
        }
    }
}
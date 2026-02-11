package com.ecommerce.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterServiceTest {

    private LoginRateLimiterService rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new LoginRateLimiterService();
    }

    @Test
    void isBlocked_shouldReturnFalseForNewKey() {
        boolean result = rateLimiter.isBlocked("user@test.com");

        assertThat(result).isFalse();
    }

    @Test
    void isBlocked_shouldReturnFalseAfterLessThanMaxAttempts() {
        String key = "user@test.com";

        rateLimiter.recordFailedAttempt(key);
        rateLimiter.recordFailedAttempt(key);
        rateLimiter.recordFailedAttempt(key);
        rateLimiter.recordFailedAttempt(key);

        assertThat(rateLimiter.isBlocked(key)).isFalse();
    }

    @Test
    void isBlocked_shouldReturnTrueAfterMaxAttempts() {
        String key = "user@test.com";

        for (int i = 0; i < 5; i++) {
            rateLimiter.recordFailedAttempt(key);
        }

        assertThat(rateLimiter.isBlocked(key)).isTrue();
    }

    @Test
    void recordFailedAttempt_shouldIncrementCounter() {
        String key = "user@test.com";

        rateLimiter.recordFailedAttempt(key);
        assertThat(rateLimiter.isBlocked(key)).isFalse();

        rateLimiter.recordFailedAttempt(key);
        rateLimiter.recordFailedAttempt(key);
        rateLimiter.recordFailedAttempt(key);
        rateLimiter.recordFailedAttempt(key);

        assertThat(rateLimiter.isBlocked(key)).isTrue();
    }

    @Test
    void resetAttempts_shouldUnblockUser() {
        String key = "user@test.com";

        for (int i = 0; i < 5; i++) {
            rateLimiter.recordFailedAttempt(key);
        }
        assertThat(rateLimiter.isBlocked(key)).isTrue();

        rateLimiter.resetAttempts(key);

        assertThat(rateLimiter.isBlocked(key)).isFalse();
    }

    @Test
    void getRemainingBlockSeconds_shouldReturnZeroForNewKey() {
        long remaining = rateLimiter.getRemainingBlockSeconds("unknown@test.com");

        assertThat(remaining).isZero();
    }

    @Test
    void getRemainingBlockSeconds_shouldReturnPositiveValueWhenBlocked() {
        String key = "user@test.com";

        for (int i = 0; i < 5; i++) {
            rateLimiter.recordFailedAttempt(key);
        }

        long remaining = rateLimiter.getRemainingBlockSeconds(key);

        assertThat(remaining).isPositive()
            .isLessThanOrEqualTo(300);
    }

    @Test
    void multipleUsers_shouldBeTrackedIndependently() {
        String user1 = "user1@test.com";
        String user2 = "user2@test.com";

        for (int i = 0; i < 5; i++) {
            rateLimiter.recordFailedAttempt(user1);
        }
        rateLimiter.recordFailedAttempt(user2);

        assertThat(rateLimiter.isBlocked(user1)).isTrue();
        assertThat(rateLimiter.isBlocked(user2)).isFalse();
    }

    @Test
    void resetAttempts_shouldNotAffectOtherUsers() {
        String user1 = "user1@test.com";
        String user2 = "user2@test.com";

        for (int i = 0; i < 5; i++) {
            rateLimiter.recordFailedAttempt(user1);
            rateLimiter.recordFailedAttempt(user2);
        }

        rateLimiter.resetAttempts(user1);

        assertThat(rateLimiter.isBlocked(user1)).isFalse();
        assertThat(rateLimiter.isBlocked(user2)).isTrue();
    }

    @Test
    void recordFailedAttempt_shouldHandleFirstAttempt() {
        String key = "newuser@test.com";

        rateLimiter.recordFailedAttempt(key);

        assertThat(rateLimiter.isBlocked(key)).isFalse();
        assertThat(rateLimiter.getRemainingBlockSeconds(key)).isNotNegative();
    }
}
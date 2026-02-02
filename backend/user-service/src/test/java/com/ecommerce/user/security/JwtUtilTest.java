package com.ecommerce.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
            "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60_000L);
    }

    @Test
    void generateToken_shouldStoreClaims() {
        String token = jwtUtil.generateToken("user-1", "test@mail.com", "CLIENT", "Test");

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("test@mail.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-1");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("CLIENT");
        assertThat(jwtUtil.validateToken(token, "test@mail.com")).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalseWhenExpired() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String token = jwtUtil.generateToken("user-1", "test@mail.com", "CLIENT", "Test");

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }
}

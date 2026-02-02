package com.ecommerce.product.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String secret;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        secret = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
    }

    @Test
    void validateToken_shouldReturnTrueForMatchingEmail() {
        String token = buildToken("seller@mail.com");

        assertThat(jwtUtil.validateToken(token, "seller@mail.com")).isTrue();
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-1");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("SELLER");
        assertThat(jwtUtil.extractName(token)).isEqualTo("Alice");
    }

    @Test
    void validateToken_shouldReturnFalseForDifferentEmail() {
        String token = buildToken("seller@mail.com");

        assertThat(jwtUtil.validateToken(token, "another@mail.com")).isFalse();
    }

    private String buildToken(String email) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
            .setSubject(email)
            .claim("userId", "user-1")
            .claim("role", "SELLER")
            .claim("name", "Alice")
            .setIssuedAt(new Date(System.currentTimeMillis() - 1000))
            .setExpiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }
}

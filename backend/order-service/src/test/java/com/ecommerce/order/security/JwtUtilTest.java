package com.ecommerce.order.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String secret = "myVerySecretKeyForJwtTokensThatIsLongEnoughForHS256Algorithm";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
    }

    private String generateValidToken(String email, String userId, String role, String name) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("role", role)
                .claim("name", name)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateExpiredToken(String email) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", "user-123")
                .claim("role", "CLIENT")
                .claim("name", "Test User")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void extractEmail_shouldReturnEmail() {
        String token = generateValidToken("test@example.com", "user-123", "CLIENT", "Test User");

        String email = jwtUtil.extractEmail(token);

        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    void extractUserId_shouldReturnUserId() {
        String token = generateValidToken("test@example.com", "user-123", "CLIENT", "Test User");

        String userId = jwtUtil.extractUserId(token);

        assertThat(userId).isEqualTo("user-123");
    }

    @Test
    void extractRole_shouldReturnRole() {
        String token = generateValidToken("test@example.com", "user-123", "SELLER", "Test User");

        String role = jwtUtil.extractRole(token);

        assertThat(role).isEqualTo("SELLER");
    }

    @Test
    void extractName_shouldReturnName() {
        String token = generateValidToken("test@example.com", "user-123", "CLIENT", "John Doe");

        String name = jwtUtil.extractName(token);

        assertThat(name).isEqualTo("John Doe");
    }

    @Test
    void extractExpiration_shouldReturnExpirationDate() {
        String token = generateValidToken("test@example.com", "user-123", "CLIENT", "Test User");

        Date expiration = jwtUtil.extractExpiration(token);

        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String email = "test@example.com";
        String token = generateValidToken(email, "user-123", "CLIENT", "Test User");

        Boolean isValid = jwtUtil.validateToken(token, email);

        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalseForWrongEmail() {
        String token = generateValidToken("test@example.com", "user-123", "CLIENT", "Test User");

        Boolean isValid = jwtUtil.validateToken(token, "wrong@example.com");

        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        String email = "test@example.com";
        String token = generateExpiredToken(email);

        assertThatThrownBy(() -> jwtUtil.validateToken(token, email))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void extractClaim_shouldWorkWithCustomClaims() {
        String token = generateValidToken("test@example.com", "custom-id", "ADMIN", "Custom Name");

        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        assertThat(userId).isEqualTo("custom-id");
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    void extractEmail_shouldThrowForInvalidToken() {
        String invalidToken = "invalid.token.here";

        assertThatThrownBy(() -> jwtUtil.extractEmail(invalidToken))
                .isInstanceOf(Exception.class);
    }
}
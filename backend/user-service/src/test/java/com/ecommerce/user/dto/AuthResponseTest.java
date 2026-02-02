package com.ecommerce.user.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResponseTest {

    @Test
    void fullConstructorShouldPopulateFields() {
        AuthResponse response = new AuthResponse(
            "token",
            "Bearer",
            "user-1",
            "alice@mail.com",
            "Alice",
            "CLIENT",
            "/avatars/a.png"
        );

        assertThat(response)
            .extracting(AuthResponse::getToken, AuthResponse::getRole, AuthResponse::getAvatar)
            .containsExactly("token", "CLIENT", "/avatars/a.png");
    }

    @Test
    void customConstructorShouldDefaultTypeToBearer() {
        AuthResponse response = new AuthResponse("token", "user-1", "alice@mail.com", "Alice", "SELLER", "/a.png");

        assertThat(response)
            .extracting(AuthResponse::getType, AuthResponse::getUserId)
            .containsExactly("Bearer", "user-1");
    }

    @Test
    void equalsAndHashCodeShouldUseAllFields() {
        AuthResponse first = new AuthResponse("token", "Bearer", "user-1", "alice@mail.com", "Alice", "SELLER", "/a.png");
        AuthResponse second = new AuthResponse("token", "Bearer", "user-1", "alice@mail.com", "Alice", "SELLER", "/a.png");

        assertThat(first)
            .isEqualTo(second)
            .hasSameHashCodeAs(second);
        assertThat(first.toString()).contains("alice@mail.com");

        second.setRole("CLIENT");
        assertThat(first).isNotEqualTo(second);
    }
}

package com.ecommerce.user.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    @Test
    void allArgsConstructorShouldExposeValues() {
        LoginRequest request = new LoginRequest("alice@mail.com", "secret");

        assertThat(request)
            .extracting(LoginRequest::getEmail, LoginRequest::getPassword)
            .containsExactly("alice@mail.com", "secret");
    }

    @Test
    void settersShouldWorkWithNoArgsConstructor() {
        LoginRequest request = new LoginRequest();
        request.setEmail("bob@mail.com");
        request.setPassword("pwd");

        assertThat(request)
            .extracting(LoginRequest::getEmail, LoginRequest::getPassword)
            .containsExactly("bob@mail.com", "pwd");
    }

    @Test
    void equalsAndHashCodeShouldWork() {
        LoginRequest first = new LoginRequest("alice@mail.com", "pwd");
        LoginRequest second = new LoginRequest("alice@mail.com", "pwd");

        assertThat(first)
            .isEqualTo(second)
            .hasSameHashCodeAs(second);
        assertThat(first.toString()).contains("alice@mail.com");

        second.setEmail("bob@mail.com");
        assertThat(first).isNotEqualTo(second);
    }
}

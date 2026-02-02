package com.ecommerce.user.dto;

import com.ecommerce.user.model.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestTest {

    @Test
    void allArgsConstructorShouldPopulateFields() {
        RegisterRequest request = new RegisterRequest("Alice", "alice@mail.com", "password123", Role.CLIENT);

        assertThat(request)
            .extracting(RegisterRequest::getName, RegisterRequest::getRole)
            .containsExactly("Alice", Role.CLIENT);
    }

    @Test
    void settersShouldAllowMutation() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Bob");
        request.setEmail("bob@mail.com");
        request.setPassword("pwd");
        request.setRole(Role.SELLER);

        assertThat(request)
            .extracting(RegisterRequest::getEmail, RegisterRequest::getRole)
            .containsExactly("bob@mail.com", Role.SELLER);
    }

    @Test
    void equalsAndHashCodeShouldDependOnValues() {
        RegisterRequest first = new RegisterRequest("Alice", "alice@mail.com", "password123", Role.CLIENT);
        RegisterRequest second = new RegisterRequest("Alice", "alice@mail.com", "password123", Role.CLIENT);

        assertThat(first)
            .isEqualTo(second)
            .hasSameHashCodeAs(second);
        assertThat(first.toString()).contains("Alice");

        second.setRole(Role.SELLER);
        assertThat(first).isNotEqualTo(second);
    }
}

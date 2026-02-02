package com.ecommerce.user.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void customConstructorShouldInitializeTimestamps() {
        User user = new User("Alice", "alice@mail.com", "hashed", Role.CLIENT);

        assertThat(user)
            .extracting(User::getName, User::getRole, User::getCreatedAt, User::getUpdatedAt)
            .containsExactly("Alice", Role.CLIENT, user.getCreatedAt(), user.getUpdatedAt());
        assertThat(user.getCreatedAt())
            .isNotNull();
        assertThat(user.getUpdatedAt())
            .isNotNull();
    }

    @Test
    void allArgsConstructorShouldExposeValues() {
        User user = new User("id", "Bob", "bob@mail.com", "pwd", Role.SELLER, "/a.png", null, null);

        assertThat(user)
            .extracting(User::getId, User::getAvatar)
            .containsExactly("id", "/a.png");
    }

    @Test
    void equalsAndHashCodeShouldDependOnFields() {
        User first = new User("id", "Bob", "bob@mail.com", "pwd", Role.SELLER, "/a.png", null, null);
        User second = new User("id", "Bob", "bob@mail.com", "pwd", Role.SELLER, "/a.png", null, null);

        assertThat(first)
            .isEqualTo(second)
            .hasSameHashCodeAs(second);
        assertThat(first.toString()).contains("bob@mail.com");

        second.setEmail("alice@mail.com");
        assertThat(first).isNotEqualTo(second);
    }
}

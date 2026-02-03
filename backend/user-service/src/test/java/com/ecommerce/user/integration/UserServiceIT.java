package com.ecommerce.user.integration;

import com.ecommerce.user.dto.*;
import com.ecommerce.user.model.Role;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceIT extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_ShouldCreateUserSuccessfully() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("Password1!");
        request.setRole(Role.CLIENT);

        String result = userService.register(request);

        assertEquals("Utilisateur créé avec succès", result);
        assertTrue(userRepository.existsByEmail("john@example.com"));
    }

    @Test
    void register_ShouldThrowOnDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("Password1!");
        request.setRole(Role.CLIENT);
        userService.register(request);

        RegisterRequest duplicate = new RegisterRequest();
        duplicate.setName("Jane Doe");
        duplicate.setEmail("john@example.com");
        duplicate.setPassword("Password2!");
        duplicate.setRole(Role.CLIENT);

        assertThrows(RuntimeException.class, () -> userService.register(duplicate));
    }

    @Test
    void login_ShouldReturnTokenForValidCredentials() {
        RegisterRequest register = new RegisterRequest();
        register.setName("John Doe");
        register.setEmail("john@example.com");
        register.setPassword("Password1!");
        register.setRole(Role.CLIENT);
        userService.register(register);

        LoginRequest login = new LoginRequest();
        login.setEmail("john@example.com");
        login.setPassword("Password1!");

        AuthResponse response = userService.login(login);

        assertNotNull(response.getToken());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("John Doe", response.getName());
        assertEquals("CLIENT", response.getRole());
    }

    @Test
    void login_ShouldThrowOnWrongPassword() {
        RegisterRequest register = new RegisterRequest();
        register.setName("John Doe");
        register.setEmail("john@example.com");
        register.setPassword("Password1!");
        register.setRole(Role.CLIENT);
        userService.register(register);

        LoginRequest login = new LoginRequest();
        login.setEmail("john@example.com");
        login.setPassword("WrongPassword1!");

        assertThrows(RuntimeException.class, () -> userService.login(login));
    }

    @Test
    void login_ShouldThrowOnNonExistentEmail() {
        LoginRequest login = new LoginRequest();
        login.setEmail("nobody@example.com");
        login.setPassword("Password1!");

        assertThrows(RuntimeException.class, () -> userService.login(login));
    }

    @Test
    void getProfile_ShouldReturnUserProfile() {
        RegisterRequest register = new RegisterRequest();
        register.setName("John Doe");
        register.setEmail("john@example.com");
        register.setPassword("Password1!");
        register.setRole(Role.SELLER);
        userService.register(register);

        UserResponse profile = userService.getProfile("john@example.com");

        assertEquals("John Doe", profile.getName());
        assertEquals("john@example.com", profile.getEmail());
        assertEquals(Role.SELLER, profile.getRole());
        assertNotNull(profile.getCreatedAt());
    }

    @Test
    void updateProfile_ShouldUpdateName() {
        RegisterRequest register = new RegisterRequest();
        register.setName("John Doe");
        register.setEmail("john@example.com");
        register.setPassword("Password1!");
        register.setRole(Role.CLIENT);
        userService.register(register);

        UserResponse updated = userService.updateProfile("john@example.com", "John Updated", null);

        assertEquals("John Updated", updated.getName());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void wishlist_ShouldAddAndRemoveProducts() {
        RegisterRequest register = new RegisterRequest();
        register.setName("John Doe");
        register.setEmail("john@example.com");
        register.setPassword("Password1!");
        register.setRole(Role.CLIENT);
        userService.register(register);

        List<String> wishlist = userService.addToWishlist("john@example.com", "product-1");
        assertEquals(1, wishlist.size());
        assertTrue(wishlist.contains("product-1"));

        wishlist = userService.addToWishlist("john@example.com", "product-2");
        assertEquals(2, wishlist.size());

        // Adding same product should not duplicate
        wishlist = userService.addToWishlist("john@example.com", "product-1");
        assertEquals(2, wishlist.size());

        assertTrue(userService.isInWishlist("john@example.com", "product-1"));
        assertFalse(userService.isInWishlist("john@example.com", "product-999"));

        wishlist = userService.removeFromWishlist("john@example.com", "product-1");
        assertEquals(1, wishlist.size());
        assertFalse(wishlist.contains("product-1"));
    }

    @Test
    void clearWishlist_ShouldEmptyWishlist() {
        RegisterRequest register = new RegisterRequest();
        register.setName("John Doe");
        register.setEmail("john@example.com");
        register.setPassword("Password1!");
        register.setRole(Role.CLIENT);
        userService.register(register);

        userService.addToWishlist("john@example.com", "product-1");
        userService.addToWishlist("john@example.com", "product-2");

        userService.clearWishlist("john@example.com");

        List<String> wishlist = userService.getWishlist("john@example.com");
        assertTrue(wishlist.isEmpty());
    }
}
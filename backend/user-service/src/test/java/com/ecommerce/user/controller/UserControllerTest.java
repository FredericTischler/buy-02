package com.ecommerce.user.controller;

import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.model.Role;
import com.ecommerce.user.model.User;
import com.ecommerce.user.security.JwtAuthenticationFilter;
import com.ecommerce.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    MongoRepositoriesAutoConfiguration.class
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestMongoConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.data.mongodb.core.mapping.MongoMappingContext mongoMappingContext() {
            return new org.springframework.data.mongodb.core.mapping.MongoMappingContext();
        }
    }

    @BeforeEach
    void setupSecurity() {
        var auth = new UsernamePasswordAuthenticationToken("alice@mail.com", "pwd");
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private UserResponse sampleResponse() {
        return new UserResponse("id", "Alice", "alice@mail.com", Role.CLIENT, "/avatar.png",
            LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void getProfile_shouldReturnProfile() throws Exception {
        when(userService.getProfile("alice@mail.com")).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/users/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("alice@mail.com"));
    }

    @Test
    void getProfile_shouldReturn404WhenMissing() throws Exception {
        when(userService.getProfile("alice@mail.com")).thenThrow(new RuntimeException("missing"));

        mockMvc.perform(get("/api/users/profile"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("missing"));
    }

    @Test
    void updateProfile_shouldReturnUpdatedProfile() throws Exception {
        when(userService.updateProfile("alice@mail.com", "New", "/img.png")).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "New", "avatar", "/img.png"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("id"));
    }

    @Test
    void updateProfile_shouldReturnBadRequestWhenServiceThrows() throws Exception {
        when(userService.updateProfile("alice@mail.com", "New", "/img.png"))
            .thenThrow(new RuntimeException("Invalid data"));

        mockMvc.perform(put("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "New", "avatar", "/img.png"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid data"));
    }

    @Test
    void uploadAvatar_shouldReturnUrl() throws Exception {
        when(userService.uploadAvatar(any(), any())).thenReturn("/uploads/avatars/file.png");

        mockMvc.perform(multipart("/api/users/avatar").file("file", "img".getBytes()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.avatarUrl").value("/uploads/avatars/file.png"));
    }

    @Test
    void uploadAvatar_shouldReturnBadRequestWhenServiceThrows() throws Exception {
        when(userService.uploadAvatar(any(), any())).thenThrow(new RuntimeException("invalid file"));

        mockMvc.perform(multipart("/api/users/avatar").file("file", "img".getBytes()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid file"));
    }

    @Test
    void getUserById_shouldReturnUserWhenPresent() throws Exception {
        User user = new User();
        user.setId("id");
        user.setName("Alice");
        user.setEmail("alice@mail.com");
        user.setRole(Role.CLIENT);
        when(userService.getUserById("id")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/id"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void getUserById_shouldReturn404WhenMissing() throws Exception {
        when(userService.getUserById("id")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/id"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Utilisateur non trouvé"));
    }

    @Test
    void getUserById_shouldReturnServerErrorOnException() throws Exception {
        when(userService.getUserById("id")).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/users/id"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("boom"));
    }

    // ==================== WISHLIST CONTROLLER TESTS ====================

    @Test
    void getWishlist_shouldReturnWishlist() throws Exception {
        List<String> wishlist = Arrays.asList("product-1", "product-2");
        when(userService.getWishlist("alice@mail.com")).thenReturn(wishlist);

        mockMvc.perform(get("/api/users/wishlist"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value("product-1"))
            .andExpect(jsonPath("$[1]").value("product-2"));
    }

    @Test
    void getWishlist_shouldReturnBadRequestWhenServiceThrows() throws Exception {
        when(userService.getWishlist("alice@mail.com")).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/api/users/wishlist"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    void addToWishlist_shouldReturnUpdatedWishlist() throws Exception {
        List<String> wishlist = Arrays.asList("product-1", "product-2");
        when(userService.addToWishlist("alice@mail.com", "product-2")).thenReturn(wishlist);

        mockMvc.perform(post("/api/users/wishlist/product-2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Produit ajouté à la wishlist"))
            .andExpect(jsonPath("$.wishlist[0]").value("product-1"));
    }

    @Test
    void addToWishlist_shouldReturnBadRequestWhenServiceThrows() throws Exception {
        when(userService.addToWishlist("alice@mail.com", "product-1"))
            .thenThrow(new RuntimeException("Product already in wishlist"));

        mockMvc.perform(post("/api/users/wishlist/product-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Product already in wishlist"));
    }

    @Test
    void removeFromWishlist_shouldReturnUpdatedWishlist() throws Exception {
        List<String> wishlist = Arrays.asList("product-2");
        when(userService.removeFromWishlist("alice@mail.com", "product-1")).thenReturn(wishlist);

        mockMvc.perform(delete("/api/users/wishlist/product-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Produit retiré de la wishlist"))
            .andExpect(jsonPath("$.wishlist[0]").value("product-2"));
    }

    @Test
    void removeFromWishlist_shouldReturnBadRequestWhenServiceThrows() throws Exception {
        when(userService.removeFromWishlist("alice@mail.com", "product-1"))
            .thenThrow(new RuntimeException("Product not in wishlist"));

        mockMvc.perform(delete("/api/users/wishlist/product-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Product not in wishlist"));
    }

    @Test
    void checkWishlist_shouldReturnTrueWhenProductInWishlist() throws Exception {
        when(userService.isInWishlist("alice@mail.com", "product-1")).thenReturn(true);

        mockMvc.perform(get("/api/users/wishlist/check/product-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inWishlist").value(true));
    }

    @Test
    void checkWishlist_shouldReturnFalseWhenProductNotInWishlist() throws Exception {
        when(userService.isInWishlist("alice@mail.com", "product-1")).thenReturn(false);

        mockMvc.perform(get("/api/users/wishlist/check/product-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inWishlist").value(false));
    }

    @Test
    void checkWishlist_shouldReturnBadRequestWhenServiceThrows() throws Exception {
        when(userService.isInWishlist("alice@mail.com", "product-1"))
            .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/api/users/wishlist/check/product-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    void clearWishlist_shouldReturnSuccessMessage() throws Exception {
        doNothing().when(userService).clearWishlist("alice@mail.com");

        mockMvc.perform(delete("/api/users/wishlist"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Wishlist vidée"));
    }

    @Test
    void clearWishlist_shouldReturnBadRequestWhenServiceThrows() throws Exception {
        doThrow(new RuntimeException("User not found")).when(userService).clearWishlist("alice@mail.com");

        mockMvc.perform(delete("/api/users/wishlist"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("User not found"));
    }
}

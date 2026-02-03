package com.ecommerce.user.controller;

import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.model.Role;
import com.ecommerce.user.security.JwtAuthenticationFilter;
import com.ecommerce.user.service.LoginRateLimiterService;
import com.ecommerce.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    MongoRepositoriesAutoConfiguration.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private LoginRateLimiterService rateLimiter;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestMongoConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.data.mongodb.core.mapping.MongoMappingContext mongoMappingContext() {
            return new org.springframework.data.mongodb.core.mapping.MongoMappingContext();
        }
    }

    @Test
    void register_shouldReturnMessage() throws Exception {
        // Password must match complexity: uppercase, lowercase, digit, special char
        RegisterRequest request = new RegisterRequest("Alice", "alice@mail.com", "Password123!", Role.CLIENT);
        Mockito.when(userService.register(any(RegisterRequest.class))).thenReturn("OK");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    void register_shouldHandleBusinessError() throws Exception {
        // Password must match complexity: uppercase, lowercase, digit, special char
        RegisterRequest request = new RegisterRequest("Alice", "alice@mail.com", "Password123!", Role.CLIENT);
        Mockito.when(userService.register(any(RegisterRequest.class))).thenThrow(new RuntimeException("exists"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("exists"));
    }

    @Test
    void login_shouldReturnAuthResponse() throws Exception {
        AuthResponse response = new AuthResponse("token", "id", "mail", "name", "CLIENT", null);
        Mockito.when(userService.login(any(LoginRequest.class))).thenReturn(response);

        LoginRequest request = new LoginRequest("user@mail.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("token"))
            .andExpect(jsonPath("$.userId").value("id"));
    }

    @Test
    void login_shouldHandleInvalidCredentials() throws Exception {
        Mockito.when(userService.login(any(LoginRequest.class))).thenThrow(new RuntimeException("invalid"));

        LoginRequest request = new LoginRequest("user@mail.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("invalid"));
    }

    @Test
    void health_shouldReturnStatus() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("User Service is running"));
    }
}

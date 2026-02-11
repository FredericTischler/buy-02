package com.ecommerce.order.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldSetAuthenticationForValidToken() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String email = "test@example.com";
        String userId = "user-123";
        String role = "CLIENT";
        String name = "Test User";

        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.extractRole(token)).thenReturn(role);
        when(jwtUtil.extractUserId(token)).thenReturn(userId);
        when(jwtUtil.extractName(token)).thenReturn(name);
        when(jwtUtil.validateToken(token, email)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(email);
        assertThat(request.getAttribute("userId")).isEqualTo(userId);
        assertThat(request.getAttribute("userRole")).isEqualTo(role);
        assertThat(request.getAttribute("userName")).isEqualTo(name);
        assertThat(request.getAttribute("userEmail")).isEqualTo(email);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotSetAuthenticationForInvalidToken() throws ServletException, IOException {
        String token = "invalid.jwt.token";
        String email = "test@example.com";

        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.extractRole(token)).thenReturn("CLIENT");
        when(jwtUtil.extractUserId(token)).thenReturn("user-123");
        when(jwtUtil.extractName(token)).thenReturn("Test User");
        when(jwtUtil.validateToken(token, email)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldContinueChainWhenNoAuthorizationHeader() throws ServletException, IOException {
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void doFilterInternal_shouldContinueChainWhenAuthorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic someToken");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void doFilterInternal_shouldHandleExceptionDuringTokenExtraction() throws ServletException, IOException {
        String token = "malformed.jwt.token";

        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractEmail(token)).thenThrow(new RuntimeException("Malformed token"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotOverrideExistingAuthentication() throws ServletException, IOException {
        String token = "valid.jwt.token";

        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractEmail(token)).thenReturn("test@example.com");
        when(jwtUtil.extractRole(token)).thenReturn("CLIENT");
        when(jwtUtil.extractUserId(token)).thenReturn("user-123");
        when(jwtUtil.extractName(token)).thenReturn("Test User");

        // Set existing authentication
        var existingAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "existing@example.com", null, java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Should not override existing authentication
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("existing@example.com");
        verify(jwtUtil, never()).validateToken(anyString(), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldSetCorrectRoleAuthority() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String email = "seller@example.com";

        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.extractRole(token)).thenReturn("SELLER");
        when(jwtUtil.extractUserId(token)).thenReturn("seller-123");
        when(jwtUtil.extractName(token)).thenReturn("Seller User");
        when(jwtUtil.validateToken(token, email)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_SELLER");
    }
}
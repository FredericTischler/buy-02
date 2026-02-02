package com.ecommerce.order.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT AUTHENTICATION FILTER
 *
 * Intercepts each HTTP request to validate JWT token.
 *
 * Process:
 * 1. Extract "Authorization: Bearer <token>" header
 * 2. Validate token with JwtUtil
 * 3. If valid, set user in SecurityContext
 * 4. Otherwise, deny access
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. GET AUTHORIZATION HEADER
        final String authorizationHeader = request.getHeader("Authorization");

        String email = null;
        String jwt = null;
        String role = null;
        String userId = null;
        String userName = null;

        // 2. EXTRACT TOKEN (format: "Bearer eyJhbGciOiJIUzI1NiJ9...")
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);  // Remove "Bearer "

            try {
                email = jwtUtil.extractEmail(jwt);
                role = jwtUtil.extractRole(jwt);
                userId = jwtUtil.extractUserId(jwt);
                userName = jwtUtil.extractName(jwt);
            } catch (Exception e) {
                logger.error("Error extracting JWT claims: " + e.getMessage());
            }
        }

        // 3. VALIDATE TOKEN AND CREATE AUTHENTICATION
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            if (jwtUtil.validateToken(jwt, email)) {

                // Create authority based on role (CLIENT or SELLER)
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                // Create authentication object
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.singletonList(authority)
                    );

                // Add details (IP, session, etc.)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Store user info in request attributes for controllers
                request.setAttribute("userId", userId);
                request.setAttribute("userRole", role);
                request.setAttribute("userName", userName);
                request.setAttribute("userEmail", email);

                // Set authentication in Spring Security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 4. PASS TO NEXT FILTER
        filterChain.doFilter(request, response);
    }
}
package com.ecommerce.media.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SECURITY CONFIGURATION
 * 
 * Configure Spring Security pour Media Service :
 * 1. Routes publiques (GET media)
 * 2. Routes protégées (POST/DELETE pour SELLER uniquement)
 * 3. JWT validation
 * 4. CORS pour Angular
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOriginsProperty;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF : Désactivé (JWT)
            .csrf(csrf -> csrf.disable())
            
            // CORS : Activé
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // SECURITY HEADERS
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(contentType -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            )

            // AUTORISATION DES ROUTES
            .authorizeHttpRequests(auth -> auth
                // ROUTES PUBLIQUES
                .requestMatchers(
                    "/api/media/product/**",   // GET media by productId (public)
                    "/api/media/file/**",      // Download file (public)
                    "/api/media/health",       // Health check
                    "/error"
                ).permitAll()
                
                // TOUTES LES AUTRES ROUTES : Authentification requise
                .anyRequest().authenticated()
            )
            
            // SESSION MANAGEMENT : STATELESS
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // AJOUTER LE FILTRE JWT
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    /**
     * CORS CONFIGURATION
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        List<String> origins = parseAllowedOrigins();
        if (origins.contains("*")) {
            configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        } else {
            configuration.setAllowedOrigins(origins);
        }
        
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept"
        ));
        
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    private List<String> parseAllowedOrigins() {
        if (allowedOriginsProperty == null || allowedOriginsProperty.isBlank()) {
            return Arrays.asList("http://localhost:4200", "https://localhost:4200", "http://localhost:8084");
        }
        return Arrays.stream(allowedOriginsProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}

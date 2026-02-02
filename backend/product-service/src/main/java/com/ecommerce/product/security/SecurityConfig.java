package com.ecommerce.product.security;

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
 * Configure Spring Security pour Product Service :
 * 1. Routes publiques (GET products)
 * 2. Routes protégées (POST/PUT/DELETE pour SELLER uniquement)
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
            
            // AUTORISATION DES ROUTES
            .authorizeHttpRequests(auth -> auth
                // ROUTES PUBLIQUES
                .requestMatchers(
                    "/api/products",           // GET all products (public)
                    "/api/products/{id}",      // GET product by ID (public)
                    "/api/products/search/**", // Search products (public)
                    "/api/products/category/**", // Products by category (public)
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
            return Collections.singletonList("*");
        }
        return Arrays.stream(allowedOriginsProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}

package com.ecommerce.user.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * Configure Spring Security pour :
 * 1. Hash les mots de passe avec BCrypt
 * 2. Activer JWT authentication
 * 3. Définir les routes publiques et protégées
 * 4. Configurer CORS (pour Angular)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Permet d'utiliser @PreAuthorize dans les controllers
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOriginsProperty;
    
    /**
     * PASSWORD ENCODER - BCrypt
     * 
     * BCrypt hash les mots de passe de façon sécurisée
     * 
     * Exemple :
     * Input  : "monPassword123"
     * Output : "$2a$10$N9qo8uLOickgx2ZMRZoMye..." (60 caractères)
     * 
     * Avantages :
     * - Chaque hash est unique (grâce au "salt")
     * - Impossible à décrypter (hash one-way)
     * - Résistant aux attaques brute-force (lent volontairement)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * AUTHENTICATION MANAGER
     * 
     * Gère l'authentification dans Spring Security
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * SECURITY FILTER CHAIN
     *
     * Configure les règles de sécurité :
     * - Routes publiques (register, login)
     * - Routes protégées (nécessitent JWT)
     * - Désactive CSRF (pas nécessaire avec JWT)
     * - Session STATELESS (pas de session serveur avec JWT)
     * - Headers de sécurité (XSS, Clickjacking, Content-Type)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF : Désactivé (JWT ne nécessite pas CSRF protection)
            .csrf(csrf -> csrf.disable())

            // CORS : Activé pour Angular
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // SECURITY HEADERS
            .headers(headers -> headers
                // Protection contre le clickjacking
                .frameOptions(frame -> frame.deny())
                // Protection XSS
                .xssProtection(xss -> xss.disable()) // Modern browsers have built-in XSS protection
                // Prevent MIME type sniffing
                .contentTypeOptions(contentType -> {})
                // Content Security Policy
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self'; " +
                    "connect-src 'self' https://localhost:* http://localhost:*"
                ))
                // Referrer Policy
                .referrerPolicy(referrer -> referrer.policy(
                    org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                ))
                // HSTS
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            )

            // AUTORISATION DES ROUTES
            .authorizeHttpRequests(auth -> auth
                // ROUTES PUBLIQUES (accessibles sans token)
                .requestMatchers(
                    "/api/auth/register",      // Inscription
                    "/api/auth/login",         // Connexion
                    "/api/auth/health",        // Health check
                    "/uploads/avatars/**",     // Avatars statiques
                    "/error"                   // Page d'erreur
                ).permitAll()

                // TOUTES LES AUTRES ROUTES : Authentification requise
                .anyRequest().authenticated()
            )

            // SESSION MANAGEMENT : STATELESS
            // Pas de session stockée côté serveur (JWT = stateless)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // AJOUTER LE FILTRE JWT
            // Le filtre s'exécute AVANT UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    /**
     * CORS CONFIGURATION
     * 
     * Permet à Angular (localhost:4200) d'accéder aux APIs
     * 
     * CORS = Cross-Origin Resource Sharing
     * Nécessaire car Frontend et Backend sont sur des ports différents
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origines autorisées (Angular en dev)
        List<String> origins = parseAllowedOrigins();
        if (origins.contains("*")) {
            configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        } else {
            configuration.setAllowedOrigins(origins);
        }
        
        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        // Headers autorisés
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept"
        ));
        
        // Autoriser les credentials (cookies, authorization headers)
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

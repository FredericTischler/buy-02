package com.ecommerce.user.controller;

import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.service.LoginRateLimiterService;
import com.ecommerce.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AUTH CONTROLLER
 * 
 * Gère les APIs d'authentification :
 * - POST /api/auth/register → Inscription
 * - POST /api/auth/login    → Connexion
 * 
 * Ces routes sont PUBLIQUES (pas besoin de JWT)
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")  // Permet CORS (Angular peut appeler les APIs)
public class AuthController {
    
    @Autowired
    private UserService userService;

    @Autowired
    private LoginRateLimiterService rateLimiter;
    
    /**
     * API : INSCRIPTION
     * 
     * POST /api/auth/register
     * 
     * Body (JSON) :
     * {
     *   "name": "John Doe",
     *   "email": "john@example.com",
     *   "password": "password123",
     *   "role": "CLIENT" ou "SELLER"
     * }
     * 
     * Réponse succès (200) :
     * {
     *   "message": "Utilisateur créé avec succès"
     * }
     * 
     * Réponse erreur (400) :
     * {
     *   "error": "Cet email est déjà utilisé"
     * }
     * 
     * @Valid → Valide automatiquement les données (@NotBlank, @Email, etc.)
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            String message = userService.register(request);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", message);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * API : CONNEXION
     * 
     * POST /api/auth/login
     * 
     * Body (JSON) :
     * {
     *   "email": "john@example.com",
     *   "password": "password123"
     * }
     * 
     * Réponse succès (200) :
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "type": "Bearer",
     *   "userId": "507f1f77bcf86cd799439011",
     *   "email": "john@example.com",
     *   "name": "John Doe",
     *   "role": "CLIENT"
     * }
     * 
     * Réponse erreur (401) :
     * {
     *   "error": "Email ou mot de passe invalide"
     * }
     * 
     * Le client doit stocker le token et l'envoyer dans les requêtes suivantes :
     * Header: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String key = request.getEmail() + "|" + getClientIp(httpRequest);

        if (rateLimiter.isBlocked(key)) {
            long remaining = rateLimiter.getRemainingBlockSeconds(key);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Trop de tentatives de connexion. Réessayez dans " + remaining + " secondes.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
        }

        try {
            AuthResponse response = userService.login(request);
            rateLimiter.resetAttempts(key);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            rateLimiter.recordFailedAttempt(key);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    /**
     * API : TEST (Pour vérifier que le service fonctionne)
     * 
     * GET /api/auth/health
     * 
     * Réponse :
     * {
     *   "status": "User Service is running"
     * }
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "User Service is running");
        return ResponseEntity.ok(response);
    }
}

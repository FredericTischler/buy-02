package com.ecommerce.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO : AUTH RESPONSE
 * 
 * Réponse renvoyée au client après une connexion réussie
 * Contient le JWT token et les infos de l'utilisateur
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    /**
     * JWT Token pour l'authentification
     * Le client doit l'envoyer dans le header Authorization pour les requêtes suivantes
     */
    private String token;
    
    /**
     * Type de token (toujours "Bearer" pour JWT)
     */
    private String type = "Bearer";
    
    /**
     * ID de l'utilisateur
     */
    private String userId;
    
    /**
     * Email de l'utilisateur
     */
    private String email;
    
    /**
     * Nom de l'utilisateur
     */
    private String name;
    
    /**
     * Rôle de l'utilisateur (CLIENT ou SELLER)
     */
    private String role;
    
    /**
     * Avatar URL de l'utilisateur
     */
    private String avatar;
    
    /**
     * Constructeur avec token seulement
     */
    public AuthResponse(String token, String userId, String email, String name, String role, String avatar) {
        this.token = token;
        this.type = "Bearer";
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role;
        this.avatar = avatar;
    }
}

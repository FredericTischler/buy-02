package com.ecommerce.user.dto;

import com.ecommerce.user.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO : USER RESPONSE
 * 
 * Réponse contenant les infos publiques d'un utilisateur
 * ⚠️ Le password n'est JAMAIS inclus pour la sécurité
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private String id;
    private String name;
    private String email;
    private Role role;
    private String avatar;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // ⚠️ Pas de password ici ! Sécurité !
}

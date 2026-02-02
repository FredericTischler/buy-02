package com.ecommerce.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO : LOGIN REQUEST
 * 
 * Données envoyées par le client lors de la connexion
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    /**
     * Email de l'utilisateur
     */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;
    
    /**
     * Mot de passe
     */
    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}

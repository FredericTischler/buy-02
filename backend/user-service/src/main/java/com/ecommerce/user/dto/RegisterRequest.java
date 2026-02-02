package com.ecommerce.user.dto;

import com.ecommerce.user.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO : REGISTER REQUEST
 *
 * Données envoyées par le client lors de l'inscription
 * Contient des validations pour s'assurer que les données sont correctes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * Nom de l'utilisateur
     * Validation : Ne doit pas être vide
     */
    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String name;

    /**
     * Email de l'utilisateur
     * Validation : Doit être un email valide et non vide
     */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;

    /**
     * Mot de passe
     * Validation :
     * - Min 8 caractères
     * - Au moins une majuscule
     * - Au moins une minuscule
     * - Au moins un chiffre
     * - Au moins un caractère spécial
     */
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial (@$!%*?&)"
    )
    private String password;

    /**
     * Rôle de l'utilisateur : CLIENT ou SELLER
     * Validation : Ne doit pas être null
     */
    @NotNull(message = "Le rôle est obligatoire")
    private Role role;
}

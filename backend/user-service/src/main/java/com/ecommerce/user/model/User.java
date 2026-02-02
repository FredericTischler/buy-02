package com.ecommerce.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * ENTITÉ USER
 * 
 * Représente un utilisateur de la plateforme (Client ou Seller)
 * Stocké dans MongoDB dans la collection "users"
 */
@Data                    // Lombok: Génère automatiquement getters, setters, toString, equals, hashCode
@NoArgsConstructor       // Lombok: Génère un constructeur sans paramètres
@AllArgsConstructor      // Lombok: Génère un constructeur avec tous les paramètres
@Document(collection = "users")  // MongoDB: Indique que c'est une collection MongoDB nommée "users"
public class User {
    
    /**
     * ID unique généré automatiquement par MongoDB
     */
    @Id
    private String id;
    
    /**
     * Nom complet de l'utilisateur
     */
    private String name;
    
    /**
     * Email de l'utilisateur (UNIQUE - pas de doublons)
     */
    @Indexed(unique = true)  // MongoDB: Crée un index unique sur le champ email
    private String email;
    
    /**
     * Mot de passe HASHÉ avec BCrypt
     * ⚠️ Ne JAMAIS stocker en clair !
     */
    private String password;
    
    /**
     * Rôle de l'utilisateur : CLIENT ou SELLER
     */
    private Role role;
    
    /**
     * Chemin vers l'avatar de l'utilisateur (optionnel)
     * Exemple : "uploads/avatars/user123.jpg"
     */
    private String avatar;
    
    /**
     * Date de création du compte
     */
    private LocalDateTime createdAt;
    
    /**
     * Date de dernière modification du profil
     */
    private LocalDateTime updatedAt;
    
    /**
     * Constructeur personnalisé pour la création d'un nouvel utilisateur
     */
    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}

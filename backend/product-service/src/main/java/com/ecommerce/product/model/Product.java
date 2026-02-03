package com.ecommerce.product.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * PRODUCT ENTITY
 *
 * Représente un produit dans la base MongoDB.
 *
 * Règles métier :
 * - Seul un SELLER peut créer/modifier/supprimer un produit
 * - Seul le SELLER propriétaire (sellerId) peut modifier/supprimer son produit
 * - Les produits sont visibles par tous (public)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id;  // MongoDB auto-generated ID

    @Indexed
    private String name;  // Nom du produit (ex: "iPhone 15 Pro")

    private String description;  // Description détaillée

    @Indexed
    private Double price;  // Prix en euros (ex: 1299.99)

    @Indexed
    private String category;  // Catégorie (ex: "Electronics", "Fashion", "Food")

    private Integer stock;  // Quantité en stock (ex: 50)

    @Indexed
    private String sellerId;  // ID du vendeur (référence vers User)
    
    private String sellerName;  // Nom du vendeur (pour affichage)
    
    private LocalDateTime createdAt;  // Date de création
    
    private LocalDateTime updatedAt;  // Date de dernière modification
    
    /**
     * Méthode appelée automatiquement avant la sauvegarde
     */
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }
}

package com.ecommerce.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PRODUCT RESPONSE DTO
 * 
 * Utilisé pour renvoyer les informations d'un produit.
 * Contient toutes les infos nécessaires pour l'affichage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    
    private String id;
    private String name;
    private String description;
    private Double price;
    private String category;
    private Integer stock;
    private String sellerId;
    private String sellerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

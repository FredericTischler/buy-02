package com.ecommerce.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PRODUCT EVENT DTO
 * 
 * Événement Kafka envoyé quand un produit est créé/modifié/supprimé.
 * Le Media Service écoute ces événements.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent {
    
    private String eventType;  // "CREATED", "UPDATED", "DELETED"
    private String productId;
    private String productName;
    private String sellerId;
    private LocalDateTime timestamp;
}

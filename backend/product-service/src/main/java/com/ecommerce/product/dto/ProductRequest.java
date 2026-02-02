package com.ecommerce.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PRODUCT REQUEST DTO
 * 
 * Utilisé pour créer ou modifier un produit.
 * Contient les validations des données d'entrée.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    
    @NotBlank(message = "Product name is required")
    private String name;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be positive")
    private Double price;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be positive or zero")
    private Integer stock;
}

package com.ecommerce.product.repository;

import com.ecommerce.product.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PRODUCT REPOSITORY
 * 
 * Interface pour accéder aux produits dans MongoDB.
 * Spring Data génère automatiquement l'implémentation.
 */
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    
    /**
     * Trouver tous les produits d'un vendeur
     * 
     * @param sellerId ID du vendeur
     * @return Liste des produits du vendeur
     */
    List<Product> findBySellerId(String sellerId);
    
    /**
     * Trouver les produits par catégorie
     * 
     * @param category Catégorie (ex: "Electronics")
     * @return Liste des produits de cette catégorie
     */
    List<Product> findByCategory(String category);
    
    /**
     * Trouver les produits par nom (recherche partielle, insensible à la casse)
     * 
     * @param name Nom ou partie du nom
     * @return Liste des produits correspondants
     */
    List<Product> findByNameContainingIgnoreCase(String name);
}

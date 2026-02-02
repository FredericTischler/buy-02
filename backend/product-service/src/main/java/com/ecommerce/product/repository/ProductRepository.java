package com.ecommerce.product.repository;

import com.ecommerce.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
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

    /**
     * Pagination pour tous les produits
     */
    Page<Product> findAll(Pageable pageable);

    /**
     * Filtrer par prix (entre min et max)
     */
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);

    /**
     * Filtrer par stock disponible (>0)
     */
    List<Product> findByStockGreaterThan(Integer stock);

    /**
     * Filtrer par catégorie avec pagination
     */
    Page<Product> findByCategory(String category, Pageable pageable);

    /**
     * Recherche avancée avec filtres multiples
     */
    @Query("{ $and: [ " +
           "{ $or: [ { 'name': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } } ] }, " +
           "{ 'price': { $gte: ?1, $lte: ?2 } }, " +
           "{ 'stock': { $gte: ?3 } } " +
           "] }")
    Page<Product> searchWithFilters(String keyword, Double minPrice, Double maxPrice, Integer minStock, Pageable pageable);

    /**
     * Recherche par catégorie avec filtres
     */
    @Query("{ $and: [ " +
           "{ 'category': ?0 }, " +
           "{ 'price': { $gte: ?1, $lte: ?2 } }, " +
           "{ 'stock': { $gte: ?3 } } " +
           "] }")
    Page<Product> findByCategoryWithFilters(String category, Double minPrice, Double maxPrice, Integer minStock, Pageable pageable);
}

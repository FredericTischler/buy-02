package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductEvent;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PRODUCT SERVICE
 * 
 * Logique métier pour la gestion des produits :
 * - CRUD complet
 * - Validation ownership (seul le SELLER propriétaire peut modifier/supprimer)
 * - Envoi d'événements Kafka
 */
@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private KafkaTemplate<String, ProductEvent> kafkaTemplate;
    
    @Value("${kafka.topic.product-events}")
    private String productEventsTopic;
    
    /**
     * CRÉER UN PRODUIT (SELLER uniquement)
     * 
     * @param request Données du produit
     * @param userId ID du vendeur (extrait du JWT)
     * @param userName Nom du vendeur
     * @return Produit créé
     */
    public ProductResponse createProduct(ProductRequest request, String userId, String userName) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStock(request.getStock());
        product.setSellerId(userId);
        product.setSellerName(userName);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        
        Product savedProduct = productRepository.save(product);
        
        // ENVOYER ÉVÉNEMENT KAFKA
        sendProductEvent("CREATED", savedProduct);
        
        return toResponse(savedProduct);
    }
    
    /**
     * RÉCUPÉRER TOUS LES PRODUITS
     */
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * RÉCUPÉRER UN PRODUIT PAR ID
     */
    public Optional<ProductResponse> getProductById(String id) {
        return productRepository.findById(id)
                .map(this::toResponse);
    }
    
    /**
     * RÉCUPÉRER LES PRODUITS D'UN VENDEUR
     */
    public List<ProductResponse> getProductsBySeller(String sellerId) {
        return productRepository.findBySellerId(sellerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * RÉCUPÉRER LES PRODUITS PAR CATÉGORIE
     */
    public List<ProductResponse> getProductsByCategory(String category) {
        return productRepository.findByCategory(category)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * RECHERCHER DES PRODUITS PAR NOM
     */
    public List<ProductResponse> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * MODIFIER UN PRODUIT (SELLER propriétaire uniquement)
     * 
     * @param id ID du produit
     * @param request Nouvelles données
     * @param userId ID du vendeur (pour vérifier ownership)
     * @return Produit modifié
     * @throws RuntimeException si l'utilisateur n'est pas le propriétaire
     */
    public ProductResponse updateProduct(String id, ProductRequest request, String userId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // VÉRIFIER OWNERSHIP
        if (!product.getSellerId().equals(userId)) {
            throw new RuntimeException("You are not authorized to update this product");
        }
        
        // METTRE À JOUR
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStock(request.getStock());
        product.setUpdatedAt(LocalDateTime.now());
        
        Product updatedProduct = productRepository.save(product);
        
        // ENVOYER ÉVÉNEMENT KAFKA
        sendProductEvent("UPDATED", updatedProduct);
        
        return toResponse(updatedProduct);
    }
    
    /**
     * SUPPRIMER UN PRODUIT (SELLER propriétaire uniquement)
     * 
     * @param id ID du produit
     * @param userId ID du vendeur (pour vérifier ownership)
     * @throws RuntimeException si l'utilisateur n'est pas le propriétaire
     */
    public void deleteProduct(String id, String userId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // VÉRIFIER OWNERSHIP
        if (!product.getSellerId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this product");
        }
        
        productRepository.delete(product);
        
        // ENVOYER ÉVÉNEMENT KAFKA
        sendProductEvent("DELETED", product);
    }
    
    /**
     * ENVOYER UN ÉVÉNEMENT KAFKA
     */
    private void sendProductEvent(String eventType, Product product) {
        ProductEvent event = new ProductEvent(
            eventType,
            product.getId(),
            product.getName(),
            product.getSellerId(),
            LocalDateTime.now()
        );
        
        kafkaTemplate.send(productEventsTopic, event);
    }
    
    /**
     * CONVERTIR PRODUCT EN PRODUCTRESPONSE
     */
    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getCategory(),
            product.getStock(),
            product.getSellerId(),
            product.getSellerName(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}

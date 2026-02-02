package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PRODUCT CONTROLLER
 * 
 * REST API pour la gestion des produits.
 * 
 * Routes publiques :
 * - GET /api/products (liste tous les produits)
 * - GET /api/products/{id} (détails d'un produit)
 * - GET /api/products/search?keyword=xxx (recherche)
 * - GET /api/products/category/{category} (par catégorie)
 * 
 * Routes protégées (SELLER uniquement) :
 * - POST /api/products (créer un produit)
 * - PUT /api/products/{id} (modifier son produit)
 * - DELETE /api/products/{id} (supprimer son produit)
 * - GET /api/products/seller/my-products (ses produits)
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    /**
     * GET ALL PRODUCTS (PUBLIC)
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }
    
    /**
     * GET PRODUCT BY ID (PUBLIC)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable String id) {
        var productOpt = productService.getProductById(id);
        if (productOpt.isPresent()) {
            return ResponseEntity.ok(productOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Product not found"));
        }
    }
    
    /**
     * SEARCH PRODUCTS (PUBLIC)
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam String keyword) {
        List<ProductResponse> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(products);
    }
    
    /**
     * GET PRODUCTS BY CATEGORY (PUBLIC)
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable String category) {
        List<ProductResponse> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(products);
    }
    
    /**
     * CREATE PRODUCT (SELLER ONLY)
     */
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> createProduct(
            @Valid @RequestBody ProductRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Récupérer userId et userName depuis le JWT (mis par le filter)
            String userId = (String) httpRequest.getAttribute("userId");
            String userName = (String) httpRequest.getAttribute("userName");
            
            ProductResponse product = productService.createProduct(request, userId, userName);
            return ResponseEntity.status(HttpStatus.CREATED).body(product);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * GET MY PRODUCTS (SELLER ONLY)
     */
    @GetMapping("/seller/my-products")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<ProductResponse>> getMyProducts(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        List<ProductResponse> products = productService.getProductsBySeller(userId);
        return ResponseEntity.ok(products);
    }
    
    /**
     * UPDATE PRODUCT (SELLER ONLY - must be owner)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = (String) httpRequest.getAttribute("userId");
            ProductResponse product = productService.updateProduct(id, request, userId);
            return ResponseEntity.ok(product);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            
            if (e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * DELETE PRODUCT (SELLER ONLY - must be owner)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> deleteProduct(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = (String) httpRequest.getAttribute("userId");
            productService.deleteProduct(id, userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Product deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            
            if (e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}

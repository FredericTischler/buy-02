package com.ecommerce.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * PRODUCT SERVICE APPLICATION
 * 
 * Point d'entrée du microservice Product.
 * 
 * Fonctionnalités :
 * - CRUD complet des produits
 * - Authentification JWT
 * - Validation rôle SELLER
 * - Kafka producer pour événements
 * - MongoDB pour stockage
 * 
 * Port : 8082
 */
@SpringBootApplication
@EnableMongoAuditing
public class ProductServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
        System.out.println("\n✅ Product Service démarré sur http://localhost:8082\n");
    }
}

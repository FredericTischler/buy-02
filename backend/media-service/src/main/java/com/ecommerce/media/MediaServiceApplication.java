package com.ecommerce.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MEDIA SERVICE APPLICATION
 * 
 * Point d'entrée du Media Service.
 * 
 * Responsabilités :
 * - Gestion des uploads d'images produits
 * - Stockage des fichiers (système de fichiers)
 * - Métadonnées dans MongoDB
 * - Consommation d'événements Kafka (suppression de produits)
 * 
 * Port : 8083
 * Base de données : ecommerce_media
 */
@SpringBootApplication
@EnableMongoAuditing
public class MediaServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MediaServiceApplication.class, args);
        System.out.println("\n✅ Media Service démarré sur http://localhost:8083");
        System.out.println("📂 Upload directory configuré");
        System.out.println("📥 Kafka consumer actif sur topic : product-events\n");
    }
}

package com.ecommerce.media.service;

import com.ecommerce.media.dto.ProductEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * KAFKA CONSUMER SERVICE
 * 
 * Écoute les événements Kafka provenant du Product Service.
 * 
 * Événements écoutés :
 * - "DELETED" : Quand un produit est supprimé, on supprime tous ses médias
 * 
 * Topic : product-events
 */
@Service
public class KafkaConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
    
    @Autowired
    private MediaService mediaService;
    
    /**
     * CONSOMMER les événements du topic "product-events"
     * 
     * @param event L'événement reçu (CREATED, UPDATED, DELETED)
     */
    @KafkaListener(
        topics = "${kafka.topic.product-events}",
        groupId = "${kafka.consumer.group-id}"
    )
    public void consumeProductEvent(ProductEvent event) {
        logger.info("📥 [KAFKA] Event reçu : {} pour produit {} (vendeur: {})", 
            event.getEventType(), event.getProductId(), event.getSellerId());
        
        try {
            // SI LE PRODUIT EST SUPPRIMÉ -> Supprimer tous ses médias
            if ("DELETED".equals(event.getEventType())) {
                logger.info("🗑️ [KAFKA] Suppression des médias du produit : {}", event.getProductId());
                mediaService.deleteAllByProductId(event.getProductId());
                logger.info("✅ [KAFKA] Médias supprimés avec succès pour le produit : {}", event.getProductId());
            }
            
            // Autres événements (CREATED, UPDATED) : Rien à faire pour l'instant
            
        } catch (IOException e) {
            logger.error("❌ [KAFKA] Erreur lors de la suppression des médias : {}", e.getMessage());
        }
    }
}

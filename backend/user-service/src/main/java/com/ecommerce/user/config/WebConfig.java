package com.ecommerce.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CONFIGURATION WEB
 * 
 * Configure :
 * - CORS pour autoriser les requêtes depuis le frontend
 * - Servir les fichiers statiques (avatars)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${upload.avatar-dir:./uploads/avatars}")
    private String avatarDir;
    
    /**
     * CORS : Autoriser toutes les origines en développement
     * ⚠️ En production, remplacer "*" par l'URL du frontend
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
    
    /**
     * RESSOURCES STATIQUES
     * 
     * Servir les avatars depuis le dossier uploads/avatars
     * URL : https://localhost:8081/uploads/avatars/filename.jpg
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Convertir le chemin relatif en chemin absolu
        String absolutePath = avatarDir;
        if (!avatarDir.startsWith("file:") && !avatarDir.startsWith("/") && !avatarDir.contains(":")) {
            // Chemin relatif, le convertir en absolu basé sur le répertoire de travail
            absolutePath = System.getProperty("user.dir") + "/" + avatarDir;
        }
        
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}

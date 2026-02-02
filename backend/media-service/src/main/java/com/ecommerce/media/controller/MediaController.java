package com.ecommerce.media.controller;

import com.ecommerce.media.dto.MediaResponse;
import com.ecommerce.media.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * MEDIA CONTROLLER
 * 
 * API REST pour gérer les médias (images) des produits.
 * 
 * Endpoints :
 * - POST /api/media/upload : Upload une image (SELLER uniquement)
 * - GET /api/media/product/{productId} : Liste des médias d'un produit (public)
 * - GET /api/media/file/{productId}/{filename} : Télécharger un fichier (public)
 * - DELETE /api/media/{id} : Supprimer un média (SELLER uniquement)
 */
@RestController
@RequestMapping("/api/media")
public class MediaController {
    
    @Autowired
    private MediaService mediaService;
    
    /**
     * UPLOAD une image
     * 
     * Sécurité : SELLER uniquement
     * Validations : Taille < 2MB, format image (jpg, png, webp)
     * 
     * @param file Le fichier à uploader
     * @param productId L'ID du produit associé
     * @return MediaResponse avec les informations du fichier uploadé
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("productId") String productId,
            @RequestAttribute(value = "userId", required = false) String sellerId) {
        
        try {
            // Vérifier que le userId est présent
            if (sellerId == null || sellerId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token JWT invalide ou userId manquant"));
            }
            
            MediaResponse response = mediaService.uploadMedia(file, productId, sellerId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'upload du fichier"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur serveur: " + e.getMessage()));
        }
    }
    
    /**
     * RÉCUPÉRER tous les médias d'un produit
     * 
     * Sécurité : Public (pas d'authentification requise)
     * 
     * @param productId L'ID du produit
     * @return Liste des médias
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<MediaResponse>> getMediaByProductId(@PathVariable String productId) {
        List<MediaResponse> mediaList = mediaService.getMediaByProductId(productId);
        return ResponseEntity.ok(mediaList);
    }
    
    /**
     * TÉLÉCHARGER un fichier média
     * 
     * Sécurité : Public
     * 
     * @param productId L'ID du produit
     * @param filename Le nom du fichier
     * @return Le fichier avec le bon content-type
     */
    @GetMapping("/file/{productId}/{filename}")
    public ResponseEntity<?> downloadFile(
            @PathVariable String productId,
            @PathVariable String filename) {
        
        try {
            Resource resource = mediaService.getMediaFile(productId, filename);
            
            // Déterminer le content-type
            String contentType = "application/octet-stream";
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (filename.endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.endsWith(".webp")) {
                contentType = "image/webp";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Fichier non trouvé"));
        }
    }
    
    /**
     * SUPPRIMER un média
     * 
     * Sécurité : SELLER uniquement (+ vérification de propriété)
     * 
     * @param id L'ID du média à supprimer
     * @param sellerId Le vendeur qui fait la requête (extrait du JWT)
     * @return Message de succès
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> deleteMedia(
            @PathVariable String id,
            @RequestAttribute("userId") String sellerId) {
        
        try {
            mediaService.deleteMedia(id, sellerId);
            return ResponseEntity.ok(Map.of("message", "Média supprimé avec succès"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la suppression du fichier"));
        }
    }
}

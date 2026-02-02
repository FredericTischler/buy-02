package com.ecommerce.media.service;

import com.ecommerce.media.dto.MediaResponse;
import com.ecommerce.media.model.Media;
import com.ecommerce.media.repository.MediaRepository;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MEDIA SERVICE
 * 
 * Gère l'upload, la récupération et la suppression de fichiers média.
 * 
 * Fonctionnalités :
 * - Upload d'image (< 2MB, formats: jpg, png, webp)
 * - Récupération de fichiers
 * - Suppression de fichiers (avec vérification de propriété)
 * - Suppression en cascade (quand un produit est supprimé)
 */
@Service
public class MediaService {
    
    @Autowired
    private MediaRepository mediaRepository;
    
    @Value("${upload.directory}")
    private String uploadDirectory;
    
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    
    /**
     * UPLOAD un fichier média
     * 
     * Validations :
     * - Taille < 2MB
     * - Format image (jpg, png, webp)
     * 
     * @param file Le fichier uploadé
     * @param productId L'ID du produit associé
     * @param sellerId Le vendeur qui upload (pour validation)
     * @return MediaResponse
     */
    public MediaResponse uploadMedia(MultipartFile file, String productId, String sellerId) throws IOException {
        
        // VALIDATION 1 : Fichier non vide
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }
        
        // VALIDATION 2 : Taille < 2MB
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("La taille du fichier dépasse 2 MB");
        }
        
        // VALIDATION 3 : Type de contenu
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Format non autorisé. Seuls jpg, png, webp sont acceptés");
        }
        
        // CRÉER LE DOSSIER DE DESTINATION
        Path productDir = Paths.get(uploadDirectory, productId);
        if (!Files.exists(productDir)) {
            Files.createDirectories(productDir);
        }
        
        // GÉNÉRER UN NOM DE FICHIER UNIQUE
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
        
        // SAUVEGARDER LE FICHIER
        Path filePath = productDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // CRÉER L'URL DU FICHIER (pour le frontend)
        String fileUrl = "/api/media/file/" + productId + "/" + uniqueFilename;
        
        // CRÉER L'ENTITÉ MEDIA DANS MONGODB
        Media media = new Media(
            productId,
            uniqueFilename,
            contentType,
            file.getSize(),
            sellerId,
            fileUrl
        );
        
        Media savedMedia = mediaRepository.save(media);
        
        return toResponse(savedMedia);
    }
    
    /**
     * RÉCUPÉRER tous les médias d'un produit
     */
    public List<MediaResponse> getMediaByProductId(String productId) {
        return mediaRepository.findByProductId(productId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * RÉCUPÉRER un fichier par ID (pour téléchargement)
     */
    public Resource getMediaFile(String productId, String filename) throws IOException {
        Path filePath = Paths.get(uploadDirectory, productId, filename);
        Resource resource = new UrlResource(filePath.toUri());
        
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("Fichier non trouvé : " + filename);
        }
    }
    
    /**
     * SUPPRIMER un média (avec vérification de propriété)
     */
    public void deleteMedia(String mediaId, String sellerId) throws IOException {
        Optional<Media> mediaOpt = mediaRepository.findById(mediaId);
        
        if (mediaOpt.isEmpty()) {
            throw new IllegalArgumentException("Média non trouvé");
        }
        
        Media media = mediaOpt.get();
        
        // VÉRIFICATION : Le vendeur est-il propriétaire ?
        if (!media.getUploadedBy().equals(sellerId)) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à supprimer ce média");
        }
        
        // SUPPRIMER LE FICHIER PHYSIQUE
        Path filePath = Paths.get(uploadDirectory, media.getProductId(), media.getFilename());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
        
        // SUPPRIMER L'ENTRÉE MONGODB
        mediaRepository.delete(media);
    }
    
    /**
     * SUPPRIMER tous les médias d'un produit (appelé par Kafka Consumer)
     */
    @Transactional
    public void deleteAllByProductId(String productId) throws IOException {
        List<Media> mediaList = mediaRepository.findByProductId(productId);
        
        for (Media media : mediaList) {
            // SUPPRIMER LE FICHIER PHYSIQUE
            Path filePath = Paths.get(uploadDirectory, media.getProductId(), media.getFilename());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        }
        
        // SUPPRIMER LES ENTRÉES MONGODB
        mediaRepository.deleteAllByProductId(productId);
        
        // SUPPRIMER LE DOSSIER SI VIDE
        Path productDir = Paths.get(uploadDirectory, productId);
        if (Files.exists(productDir) && Files.list(productDir).count() == 0) {
            Files.delete(productDir);
        }
    }
    
    /**
     * CONVERTIR Media -> MediaResponse
     */
    private MediaResponse toResponse(Media media) {
        return new MediaResponse(
            media.getId(),
            media.getProductId(),
            media.getFilename(),
            media.getContentType(),
            media.getSize(),
            media.getUploadedBy(),
            media.getUrl(),
            media.getUploadedAt()
        );
    }
}

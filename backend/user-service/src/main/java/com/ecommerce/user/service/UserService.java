package com.ecommerce.user.service;

import com.ecommerce.user.dto.*;
import com.ecommerce.user.model.User;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * USER SERVICE
 * 
 * Contient toute la logique métier pour la gestion des utilisateurs
 * 
 * Responsabilités :
 * - Inscription (register)
 * - Connexion (login)
 * - Gestion du profil
 * - Hash des mots de passe
 * - Génération des tokens JWT
 */
@Service
@Transactional  // Gère automatiquement les transactions de la base de données
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;  // BCrypt
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Value("${upload.avatar-dir:./uploads/avatars}")
    private String avatarDir;
    
    /**
     * INSCRIPTION (REGISTER)
     * 
     * Processus :
     * 1. Vérifier si l'email existe déjà
     * 2. Hash le mot de passe avec BCrypt
     * 3. Créer le User
     * 4. Sauvegarder dans MongoDB
     * 5. Retourner un message de succès
     * 
     * @param request Les données d'inscription
     * @return Message de succès
     * @throws RuntimeException si l'email existe déjà
     */
    public String register(RegisterRequest request) {
        
        // 1. VÉRIFIER SI L'EMAIL EXISTE DÉJÀ
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }
        
        // 2. HASH LE MOT DE PASSE AVEC BCRYPT
        // Exemple : "password123" → "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        
        // 3. CRÉER L'UTILISATEUR
        User user = new User(
            request.getName(),
            request.getEmail(),
            hashedPassword,  // Password hashé, pas en clair !
            request.getRole()
        );
        
        // 4. SAUVEGARDER DANS MONGODB
        userRepository.save(user);
        
        // 5. RETOURNER UN MESSAGE DE SUCCÈS
        return "Utilisateur créé avec succès";
    }
    
    /**
     * CONNEXION (LOGIN)
     * 
     * Processus :
     * 1. Chercher l'utilisateur par email
     * 2. Vérifier si l'utilisateur existe
     * 3. Comparer le password saisi avec le hash en DB
     * 4. Si OK → Générer un JWT token
     * 5. Retourner le token + infos user
     * 
     * @param request Les données de connexion
     * @return AuthResponse avec le token JWT
     * @throws RuntimeException si email ou password invalide
     */
    public AuthResponse login(LoginRequest request) {
        
        // 1. CHERCHER L'UTILISATEUR PAR EMAIL
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Email ou mot de passe invalide"));
        
        // 2. VÉRIFIER LE MOT DE PASSE
        // passwordEncoder.matches() compare le password saisi avec le hash en DB
        // "password123" + hash en DB → true si correspond
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Email ou mot de passe invalide");
        }
        
        // 3. GÉNÉRER LE JWT TOKEN
        String token = jwtUtil.generateToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name(),
            user.getName()
        );
        
        // 4. RETOURNER LA RÉPONSE AVEC LE TOKEN
        return new AuthResponse(
            token,
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole().name(),
            user.getAvatar()
        );
    }
    
    /**
     * RÉCUPÉRER LE PROFIL UTILISATEUR
     * 
     * @param email Email de l'utilisateur (extrait du JWT)
     * @return UserResponse avec les infos publiques
     * @throws RuntimeException si l'utilisateur n'existe pas
     */
    public UserResponse getProfile(String email) {
        
        // Chercher l'utilisateur par email
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        // Convertir User → UserResponse (sans le password !)
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getAvatar(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
    
    /**
     * METTRE À JOUR LE PROFIL
     * 
     * @param email Email de l'utilisateur (extrait du JWT)
     * @param name Nouveau nom
     * @param avatar Nouveau chemin d'avatar
     * @return UserResponse mis à jour
     * @throws RuntimeException si l'utilisateur n'existe pas
     */
    public UserResponse updateProfile(String email, String name, String avatar) {
        
        // Chercher l'utilisateur
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        // Mettre à jour les champs
        if (name != null && !name.isEmpty()) {
            user.setName(name);
        }
        
        if (avatar != null && !avatar.isEmpty()) {
            user.setAvatar(avatar);
        }
        
        // Mettre à jour la date de modification
        user.setUpdatedAt(LocalDateTime.now());
        
        // Sauvegarder
        userRepository.save(user);
        
        // Retourner le profil mis à jour
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getAvatar(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
    
    /**
     * RÉCUPÉRER UN UTILISATEUR PAR ID
     * 
     * @param id ID de l'utilisateur
     * @return User ou null
     */
    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }
    
    /**
     * UPLOAD AVATAR
     * 
     * @param email Email de l'utilisateur
     * @param file Fichier image
     * @return URL de l'avatar
     */
    public String uploadAvatar(String email, MultipartFile file) {
        // Validation du fichier
        if (file.isEmpty()) {
            throw new RuntimeException("Le fichier est vide");
        }
        
        // Vérifier la taille (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("La taille du fichier ne doit pas dépasser 5MB");
        }
        
        // Vérifier le type de fichier
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Le fichier doit être une image");
        }
        
        // Chercher l'utilisateur
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        try {
            // Créer le dossier s'il n'existe pas
            Path uploadPath = Paths.get(avatarDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Générer un nom unique pour le fichier
            String extension = getFileExtension(file.getOriginalFilename());
            String filename = user.getId() + "_" + UUID.randomUUID().toString() + extension;
            
            // Sauvegarder le fichier
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);
            
            // Construire l'URL relative
            String avatarUrl = "/uploads/avatars/" + filename;
            
            // Mettre à jour l'utilisateur
            user.setAvatar(avatarUrl);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            return avatarUrl;
            
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload de l'avatar: " + e.getMessage());
        }
    }
    
    /**
     * Extraire l'extension d'un fichier
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        return (lastDot == -1) ? "" : filename.substring(lastDot);
    }
    
    /**
     * VÉRIFIER SI UN EMAIL EXISTE
     *
     * @param email Email à vérifier
     * @return true si existe, false sinon
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    // ==================== WISHLIST ====================

    /**
     * RÉCUPÉRER LA WISHLIST D'UN UTILISATEUR
     *
     * @param email Email de l'utilisateur
     * @return Liste des IDs de produits
     */
    public List<String> getWishlist(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return user.getWishlist() != null ? user.getWishlist() : new ArrayList<>();
    }

    /**
     * AJOUTER UN PRODUIT À LA WISHLIST
     *
     * @param email Email de l'utilisateur
     * @param productId ID du produit à ajouter
     * @return Liste mise à jour
     */
    public List<String> addToWishlist(String email, String productId) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user.getWishlist() == null) {
            user.setWishlist(new ArrayList<>());
        }

        // Vérifier si le produit n'est pas déjà dans la wishlist
        if (!user.getWishlist().contains(productId)) {
            user.getWishlist().add(productId);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        return user.getWishlist();
    }

    /**
     * SUPPRIMER UN PRODUIT DE LA WISHLIST
     *
     * @param email Email de l'utilisateur
     * @param productId ID du produit à supprimer
     * @return Liste mise à jour
     */
    public List<String> removeFromWishlist(String email, String productId) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user.getWishlist() != null) {
            user.getWishlist().remove(productId);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        return user.getWishlist() != null ? user.getWishlist() : new ArrayList<>();
    }

    /**
     * VÉRIFIER SI UN PRODUIT EST DANS LA WISHLIST
     *
     * @param email Email de l'utilisateur
     * @param productId ID du produit
     * @return true si présent, false sinon
     */
    public boolean isInWishlist(String email, String productId) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return user.getWishlist() != null && user.getWishlist().contains(productId);
    }

    /**
     * VIDER LA WISHLIST
     *
     * @param email Email de l'utilisateur
     */
    public void clearWishlist(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setWishlist(new ArrayList<>());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}

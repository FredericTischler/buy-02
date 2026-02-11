package com.ecommerce.user.controller;

import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.model.User;
import com.ecommerce.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * USER CONTROLLER
 *
 * Gère les APIs du profil utilisateur :
 * - GET  /api/users/profile → Récupérer son profil
 * - PUT  /api/users/profile → Modifier son profil
 *
 * Ces routes sont PROTEGEES : JWT token OBLIGATOIRE !
 *
 * Le token doit être envoyé dans le header :
 * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";

    private final UserService userService;
    
    /**
     * RÉCUPÉRER L'EMAIL DE L'UTILISATEUR CONNECTÉ
     * 
     * L'email est extrait du JWT token par le JwtAuthenticationFilter
     * et stocké dans le SecurityContext de Spring Security
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();  // Retourne l'email
    }
    
    /**
     * API : RÉCUPÉRER SON PROFIL
     * 
     * GET /api/users/profile
     * Header: Authorization: Bearer <token>
     * 
     * Réponse (200) :
     * {
     *   "id": "507f1f77bcf86cd799439011",
     *   "name": "John Doe",
     *   "email": "john@example.com",
     *   "role": "CLIENT",
     *   "avatar": "uploads/avatars/user123.jpg",
     *   "createdAt": "2025-10-27T12:00:00",
     *   "updatedAt": "2025-10-27T12:00:00"
     * }
     * 
     * ⚠️ Le password n'est JAMAIS retourné !
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            String email = getCurrentUserEmail();
            UserResponse profile = userService.getProfile(email);
            
            return ResponseEntity.ok(profile);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    /**
     * API : MODIFIER SON PROFIL
     * 
     * PUT /api/users/profile
     * Header: Authorization: Bearer <token>
     * 
     * Body (JSON) - Tous les champs sont optionnels :
     * {
     *   "name": "John Updated",
     *   "avatar": "uploads/avatars/new-avatar.jpg"
     * }
     * 
     * Réponse (200) :
     * {
     *   "id": "507f1f77bcf86cd799439011",
     *   "name": "John Updated",
     *   "email": "john@example.com",
     *   "role": "CLIENT",
     *   "avatar": "uploads/avatars/new-avatar.jpg",
     *   "createdAt": "2025-10-27T12:00:00",
     *   "updatedAt": "2025-10-27T15:30:00"
     * }
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updates) {
        try {
            String email = getCurrentUserEmail();
            String name = updates.get("name");
            String avatar = updates.get("avatar");
            
            UserResponse profile = userService.updateProfile(email, name, avatar);
            
            return ResponseEntity.ok(profile);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    /**
     * API : UPLOAD AVATAR
     * 
     * POST /api/users/avatar
     * Header: Authorization: Bearer <token>
     * Content-Type: multipart/form-data
     * Body: file (image file, max 5MB)
     * 
     * Réponse (200) :
     * {
     *   "avatarUrl": "uploads/avatars/507f1f77bcf86cd799439011.jpg"
     * }
     */
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String email = getCurrentUserEmail();
            String avatarUrl = userService.uploadAvatar(email, file);
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }
    
    /**
     * API : RÉCUPÉRER UN UTILISATEUR PAR ID
     *
     * GET /api/users/{id}
     * Header: Authorization: Bearer <token>
     *
     * Utile pour les autres services (Product Service peut vérifier qu'un seller existe)
     *
     * Réponse (200) : UserResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        try {
            Optional<User> userOptional = userService.getUserById(id);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                UserResponse response = new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getAvatar(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
                );
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(ERROR_KEY, "Utilisateur non trouvé"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    // ==================== WISHLIST ====================

    /**
     * API : RÉCUPÉRER LA WISHLIST
     *
     * GET /api/users/wishlist
     * Header: Authorization: Bearer <token>
     *
     * Réponse (200) : Liste des IDs de produits
     */
    @GetMapping("/wishlist")
    public ResponseEntity<?> getWishlist() {
        try {
            String email = getCurrentUserEmail();
            List<String> wishlist = userService.getWishlist(email);
            return ResponseEntity.ok(wishlist);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    /**
     * API : AJOUTER UN PRODUIT À LA WISHLIST
     *
     * POST /api/users/wishlist/{productId}
     * Header: Authorization: Bearer <token>
     *
     * Réponse (200) : Liste mise à jour
     */
    @PostMapping("/wishlist/{productId}")
    public ResponseEntity<?> addToWishlist(@PathVariable String productId) {
        try {
            String email = getCurrentUserEmail();
            List<String> wishlist = userService.addToWishlist(email, productId);
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Produit ajouté à la wishlist", "wishlist", wishlist));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    /**
     * API : SUPPRIMER UN PRODUIT DE LA WISHLIST
     *
     * DELETE /api/users/wishlist/{productId}
     * Header: Authorization: Bearer <token>
     *
     * Réponse (200) : Liste mise à jour
     */
    @DeleteMapping("/wishlist/{productId}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable String productId) {
        try {
            String email = getCurrentUserEmail();
            List<String> wishlist = userService.removeFromWishlist(email, productId);
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Produit retiré de la wishlist", "wishlist", wishlist));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    /**
     * API : VÉRIFIER SI UN PRODUIT EST DANS LA WISHLIST
     *
     * GET /api/users/wishlist/check/{productId}
     * Header: Authorization: Bearer <token>
     *
     * Réponse (200) : { "inWishlist": true/false }
     */
    @GetMapping("/wishlist/check/{productId}")
    public ResponseEntity<?> checkWishlist(@PathVariable String productId) {
        try {
            String email = getCurrentUserEmail();
            boolean inWishlist = userService.isInWishlist(email, productId);
            return ResponseEntity.ok(Map.of("inWishlist", inWishlist));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    /**
     * API : VIDER LA WISHLIST
     *
     * DELETE /api/users/wishlist
     * Header: Authorization: Bearer <token>
     *
     * Réponse (200) : Message de confirmation
     */
    @DeleteMapping("/wishlist")
    public ResponseEntity<?> clearWishlist() {
        try {
            String email = getCurrentUserEmail();
            userService.clearWishlist(email);
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Wishlist vidée"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }
}

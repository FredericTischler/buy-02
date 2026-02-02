package com.ecommerce.user.repository;

import com.ecommerce.user.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * USER REPOSITORY
 * 
 * Interface pour accéder aux données User dans MongoDB
 * Spring Data génère automatiquement l'implémentation !
 * 
 * Méthodes disponibles automatiquement :
 * - save(user)         → Créer ou mettre à jour
 * - findById(id)       → Trouver par ID
 * - findAll()          → Trouver tous
 * - deleteById(id)     → Supprimer par ID
 * - count()            → Compter
 * etc.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    /**
     * Trouver un utilisateur par email
     * 
     * @param email Email de l'utilisateur
     * @return Optional<User> (peut être vide si pas trouvé)
     * 
     * Spring Data génère automatiquement la requête MongoDB :
     * db.users.findOne({email: "email@example.com"})
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Vérifier si un email existe déjà
     * 
     * @param email Email à vérifier
     * @return true si l'email existe, false sinon
     * 
     * Utile pour la validation lors de l'inscription
     */
    boolean existsByEmail(String email);
}

package com.ecommerce.user.model;

/**
 * ENUM ROLE
 * 
 * Définit les deux types d'utilisateurs de la plateforme :
 * - CLIENT : Peut consulter et acheter des produits
 * - SELLER : Peut créer, modifier et supprimer ses produits
 */
public enum Role {
    CLIENT,   // Utilisateur qui achète
    SELLER    // Utilisateur qui vend
}

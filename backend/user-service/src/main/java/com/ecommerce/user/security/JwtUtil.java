package com.ecommerce.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT UTILITY
 * 
 * Cette classe gère la création et la validation des JWT tokens
 * 
 * JWT (JSON Web Token) = Header + Payload + Signature
 * - Header : Type de token et algorithme
 * - Payload : Données (userId, email, role, expiration)
 * - Signature : Clé secrète pour sécuriser le token
 */
@Component
public class JwtUtil {
    
    /**
     * Clé secrète pour signer les tokens (depuis application.yml)
     * ⚠️ DOIT être longue et complexe en production !
     */
    @Value("${jwt.secret}")
    private String secret;
    
    /**
     * Durée de validité du token en millisecondes (depuis application.yml)
     * Par défaut : 86400000 ms = 24 heures
     */
    @Value("${jwt.expiration}")
    private Long expiration;
    
    /**
     * Obtenir la clé de signature à partir du secret
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    /**
     * GÉNÉRATION DU TOKEN JWT
     * 
     * @param userId ID de l'utilisateur
     * @param email Email de l'utilisateur
     * @param role Rôle (CLIENT ou SELLER)
     * @param name Nom de l'utilisateur
     * @return Le token JWT signé
     * 
     * Processus :
     * 1. Crée un Map avec les données (claims)
     * 2. Définit le sujet (email)
     * 3. Définit la date de création
     * 4. Définit la date d'expiration (maintenant + 24h)
     * 5. Signe avec la clé secrète
     */
    public String generateToken(String userId, String email, String role, String name) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("name", name);
        
        return Jwts.builder()
                .setClaims(claims)                          // Ajoute les données
                .setSubject(email)                           // Email comme sujet
                .setIssuedAt(new Date())                     // Date de création
                .setExpiration(new Date(System.currentTimeMillis() + expiration))  // Date d'expiration
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)  // Signature avec clé secrète
                .compact();
    }
    
    /**
     * EXTRAIRE L'EMAIL DU TOKEN
     * 
     * @param token Le JWT token
     * @return L'email de l'utilisateur
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * EXTRAIRE L'USER ID DU TOKEN
     */
    public String extractUserId(String token) {
        return extractAllClaims(token).get("userId", String.class);
    }
    
    /**
     * EXTRAIRE LE ROLE DU TOKEN
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }
    
    /**
     * EXTRAIRE LA DATE D'EXPIRATION
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * EXTRAIRE UN CLAIM SPÉCIFIQUE
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * EXTRAIRE TOUS LES CLAIMS DU TOKEN
     * 
     * Parse le token et récupère toutes les données
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * VÉRIFIER SI LE TOKEN EST EXPIRÉ
     * 
     * @param token Le JWT token
     * @return true si expiré, false sinon
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * VALIDER LE TOKEN
     * 
     * @param token Le JWT token
     * @param email L'email de l'utilisateur
     * @return true si le token est valide, false sinon
     * 
     * Un token est valide si :
     * 1. L'email correspond
     * 2. Le token n'est pas expiré
     */
    public Boolean validateToken(String token, String email) {
        final String extractedEmail = extractEmail(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token));
    }
    
    /**
     * VALIDER LE TOKEN (version simple)
     * 
     * Vérifie juste si le token n'est pas expiré
     */
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}

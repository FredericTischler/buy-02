package com.ecommerce.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT AUTHENTICATION FILTER
 * 
 * Ce filtre intercepte TOUTES les requêtes HTTP pour vérifier le JWT token
 * 
 * Processus :
 * 1. Client envoie une requête avec le header :
 *    Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * 
 * 2. Le filtre extrait le token
 * 3. Valide le token avec JwtUtil
 * 4. Si valide → Autorise la requête
 *    Si invalide → Bloque (401 Unauthorized)
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * Méthode appelée pour chaque requête HTTP
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        // 1. RÉCUPÉRER LE HEADER AUTHORIZATION
        final String authorizationHeader = request.getHeader("Authorization");
        
        String email = null;
        String jwt = null;
        
        // 2. VÉRIFIER SI LE HEADER CONTIENT UN TOKEN JWT
        // Format attendu : "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            // Extraire le token (enlever "Bearer ")
            jwt = authorizationHeader.substring(7);
            
            try {
                // Extraire l'email du token
                email = jwtUtil.extractEmail(jwt);
            } catch (Exception e) {
                // Token invalide ou corrompu
                logger.error("Erreur lors de l'extraction de l'email du token : " + e.getMessage());
            }
        }
        
        // 3. VALIDER LE TOKEN ET AUTHENTIFIER L'UTILISATEUR
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // Valider le token
            if (jwtUtil.validateToken(jwt, email)) {
                
                // Extraire le rôle
                String role = jwtUtil.extractRole(jwt);
                
                // Créer l'authentification Spring Security
                // SimpleGrantedAuthority = Autorité/Rôle dans Spring Security
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                
                // Ajouter les détails de la requête
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Définir l'authentification dans le contexte Spring Security
                // → L'utilisateur est maintenant authentifié !
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        // 4. CONTINUER LA CHAÎNE DE FILTRES
        // Passe la requête au prochain filtre ou au controller
        filterChain.doFilter(request, response);
    }
}

package com.ecommerce.product.security;

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
 * Intercepte chaque requête HTTP pour valider le token JWT.
 * 
 * Process :
 * 1. Extraire le header "Authorization: Bearer <token>"
 * 2. Valider le token avec JwtUtil
 * 3. Si valide, mettre l'utilisateur dans le SecurityContext
 * 4. Sinon, refuser l'accès
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // 1. RÉCUPÉRER LE HEADER AUTHORIZATION
        final String authorizationHeader = request.getHeader("Authorization");
        
        String email = null;
        String jwt = null;
        String role = null;
        String userId = null;
        String userName = null;
        
        // 2. EXTRAIRE LE TOKEN (format: "Bearer eyJhbGciOiJIUzI1NiJ9...")
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);  // Enlever "Bearer "
            
            try {
                email = jwtUtil.extractEmail(jwt);
                role = jwtUtil.extractRole(jwt);
                userId = jwtUtil.extractUserId(jwt);
                userName = jwtUtil.extractName(jwt);
            } catch (Exception e) {
                logger.error("Error extracting JWT claims: " + e.getMessage());
            }
        }
        
        // 3. VALIDER LE TOKEN ET CRÉER L'AUTHENTIFICATION
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            if (jwtUtil.validateToken(jwt, email)) {
                
                // Créer une autorité basée sur le rôle (CLIENT ou SELLER)
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                
                // Créer l'objet d'authentification
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                        email, 
                        null, 
                        Collections.singletonList(authority)
                    );
                
                // Ajouter des détails (IP, session, etc.)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Stocker le userId dans les détails pour l'utiliser dans les controllers
                request.setAttribute("userId", userId);
                request.setAttribute("userRole", role);
                request.setAttribute("userName", userName);
                
                // Mettre l'authentification dans le contexte Spring Security
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        // 4. PASSER AU FILTRE SUIVANT
        filterChain.doFilter(request, response);
    }
}

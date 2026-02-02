import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { Auth } from '../services/auth';

export const loginGuard: CanActivateFn = (route, state) => {
  const authService = inject(Auth);
  const router = inject(Router);

  // Si l'utilisateur est déjà connecté
  if (authService.isLoggedIn()) {
    const user = authService.getCurrentUser();
    
    // Rediriger selon le rôle
    if (user?.role === 'SELLER') {
      router.navigate(['/seller/dashboard']);
    } else {
      router.navigate(['/products']);
    }
    
    return false; // Bloquer l'accès à /login ou /register
  }

  // Sinon, autoriser l'accès
  return true;
};

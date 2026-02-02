import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { Auth } from '../services/auth';

export const sellerGuard: CanActivateFn = (route, state) => {
  const authService = inject(Auth);
  const router = inject(Router);

  // Vérifier si l'utilisateur est connecté
  if (!authService.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }

  // Vérifier si l'utilisateur est un SELLER
  if (authService.isSeller()) {
    return true;
  }

  // Si CLIENT, rediriger vers products
  router.navigate(['/products']);
  return false;
};

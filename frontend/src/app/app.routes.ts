import { Routes } from '@angular/router';
import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';
import { ProductList } from './features/products/product-list/product-list';
import { ProductDetail } from './features/products/product-detail/product-detail';
import { CartPage } from './features/cart/cart';
import { Dashboard } from './features/seller/dashboard/dashboard';
import { authGuard } from './core/guards/auth.guard';
import { sellerGuard } from './core/guards/seller.guard';
import { loginGuard } from './core/guards/login.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { 
    path: 'login', 
    component: Login,
    canActivate: [loginGuard] // Si déjà connecté → redirection automatique
  },
  { 
    path: 'register', 
    component: Register,
    canActivate: [loginGuard] // Si déjà connecté → redirection automatique
  },
  { 
    path: 'products', 
    component: ProductList,
    canActivate: [authGuard] // Protégé : nécessite d'être connecté
  },
  { 
    path: 'products/:id', 
    component: ProductDetail,
    canActivate: [authGuard] // Protégé : nécessite d'être connecté
  },
  { 
    path: 'cart', 
    component: CartPage,
    canActivate: [authGuard] // Protégé : nécessite d'être connecté
  },
  { 
    path: 'seller/dashboard', 
    component: Dashboard,
    canActivate: [sellerGuard] // Protégé : nécessite d'être SELLER
  },
];

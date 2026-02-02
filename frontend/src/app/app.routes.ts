import { Routes } from '@angular/router';
import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';
import { ProductList } from './features/products/product-list/product-list';
import { ProductDetail } from './features/products/product-detail/product-detail';
import { CartPage } from './features/cart/cart';
import { Dashboard } from './features/seller/dashboard/dashboard';
import { SellerOrders } from './features/seller/seller-orders/seller-orders';
import { Checkout } from './features/orders/checkout/checkout';
import { MyOrders } from './features/orders/my-orders/my-orders';
import { OrderDetail } from './features/orders/order-detail/order-detail';
import { UserProfile } from './features/profile/user-profile';
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
    path: 'checkout',
    component: Checkout,
    canActivate: [authGuard] // Protégé : nécessite d'être connecté
  },
  {
    path: 'orders',
    component: MyOrders,
    canActivate: [authGuard] // Protégé : nécessite d'être connecté
  },
  {
    path: 'orders/:id',
    component: OrderDetail,
    canActivate: [authGuard] // Protégé : nécessite d'être connecté
  },
  {
    path: 'seller/dashboard',
    component: Dashboard,
    canActivate: [sellerGuard] // Protégé : nécessite d'être SELLER
  },
  {
    path: 'seller/orders',
    component: SellerOrders,
    canActivate: [sellerGuard] // Protégé : nécessite d'être SELLER
  },
  {
    path: 'profile',
    component: UserProfile,
    canActivate: [authGuard] // Protégé : nécessite d'être connecté
  },
];

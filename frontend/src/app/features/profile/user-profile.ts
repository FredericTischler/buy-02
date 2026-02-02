import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatBadgeModule } from '@angular/material/badge';

import { Auth } from '../../core/services/auth';
import { OrderService } from '../../core/services/order';
import { Cart } from '../../core/services/cart';
import { User } from '../../core/models/user.model';
import { UserOrderStats } from '../../core/models/order.model';
import { resolveApiBase } from '../../core/utils/api-host';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatSnackBarModule,
    MatChipsModule,
    MatToolbarModule,
    MatBadgeModule
  ],
  templateUrl: './user-profile.html',
  styleUrl: './user-profile.scss'
})
export class UserProfile implements OnInit {
  user: User | null = null;
  stats: UserOrderStats | null = null;
  isLoading = true;
  statsLoading = true;
  cartCount = 0;

  private readonly userApiBase = resolveApiBase(8081);

  constructor(
    private readonly authService: Auth,
    private readonly orderService: OrderService,
    private readonly cartService: Cart,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadUserProfile();
    this.loadUserStats();
    this.updateCartCount();
    this.cartService.cartItems$.subscribe(() => {
      this.updateCartCount();
    });
  }

  loadUserProfile(): void {
    this.isLoading = true;
    this.user = this.authService.getCurrentUser();
    this.isLoading = false;

    if (!this.user) {
      this.snackBar.open('Utilisateur non connecté', 'Erreur', { duration: 3000 });
      this.router.navigate(['/login']);
    }
  }

  loadUserStats(): void {
    this.statsLoading = true;
    this.orderService.getUserStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.statsLoading = false;
      },
      error: (error) => {
        console.error('Erreur chargement stats:', error);
        this.statsLoading = false;
        this.snackBar.open('Erreur lors du chargement des statistiques', 'Fermer', {
          duration: 3000
        });
      }
    });
  }

  updateCartCount(): void {
    this.cartCount = this.cartService.getCartCount();
  }

  getAvatarUrl(): string {
    if (this.user?.avatar) {
      return `${this.userApiBase}${this.user.avatar}`;
    }
    return '';
  }

  hasAvatar(): boolean {
    return !!this.user?.avatar;
  }

  getInitials(): string {
    if (!this.user?.name) return '?';
    return this.user.name
      .split(' ')
      .map(part => part.charAt(0).toUpperCase())
      .slice(0, 2)
      .join('');
  }

  getRoleDisplay(): string {
    if (!this.user) return '';
    return this.user.role === 'SELLER' ? 'Vendeur' : 'Client';
  }

  getRoleIcon(): string {
    if (!this.user) return 'person';
    return this.user.role === 'SELLER' ? 'storefront' : 'shopping_bag';
  }

  formatDate(date: Date | undefined): string {
    if (!date) return '-';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(amount);
  }

  goToOrders(): void {
    this.router.navigate(['/orders']);
  }

  goToProducts(): void {
    this.router.navigate(['/products']);
  }

  goToCart(): void {
    this.router.navigate(['/cart']);
  }

  goToDashboard(): void {
    this.router.navigate(['/seller/dashboard']);
  }

  goToSellerOrders(): void {
    this.router.navigate(['/seller/orders']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { Auth } from '../../core/services/auth';
import { OrderService } from '../../core/services/order';
import { Cart } from '../../core/services/cart';
import { MediaService } from '../../core/services/media';
import { User } from '../../core/models/user.model';
import { UserOrderStats, UserProductStats } from '../../core/models/order.model';
import { resolveApiBase } from '../../core/utils/api-host';
import { PageHeader } from '../../shared/page-header/page-header';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatSnackBarModule,
    PageHeader
  ],
  templateUrl: './user-profile.html',
  styleUrl: './user-profile.scss'
})
export class UserProfile implements OnInit {
  user: User | null = null;
  stats: UserOrderStats | null = null;
  productStats: UserProductStats | null = null;
  isLoading = true;
  statsLoading = true;
  productStatsLoading = true;
  cartCount = 0;

  private readonly userApiBase = resolveApiBase(8081);

  constructor(
    private readonly authService: Auth,
    private readonly orderService: OrderService,
    private readonly cartService: Cart,
    private readonly mediaService: MediaService,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadUserProfile();
    this.loadUserStats();
    this.loadUserProductStats();
    this.cartService.cartItems$.subscribe(() => {
      this.cartCount = this.cartService.getCartCount();
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

  loadUserProductStats(): void {
    this.productStatsLoading = true;
    this.orderService.getUserProductStats().subscribe({
      next: (productStats) => {
        this.productStats = productStats;
        this.productStatsLoading = false;
      },
      error: (error) => {
        console.error('Erreur chargement stats produits:', error);
        this.productStatsLoading = false;
      }
    });
  }

  getProductImageUrl(imageUrl: string | undefined): string {
    if (!imageUrl) return '';
    return this.mediaService.getImageUrl(imageUrl);
  }

  goToProduct(productId: string): void {
    this.router.navigate(['/products', productId]);
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
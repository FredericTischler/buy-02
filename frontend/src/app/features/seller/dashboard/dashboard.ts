import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Product } from '../../../core/services/product';
import { Auth } from '../../../core/services/auth';
import { OrderService } from '../../../core/services/order';
import { MediaService } from '../../../core/services/media';
import { SellerOrderStats, SellerProductStats } from '../../../core/models/order.model';
import { Product as ProductModel } from '../../../core/models/product.model';
import { ProductFormDialog } from '../product-form-dialog/product-form-dialog';
import { resolveApiBase } from '../../../core/utils/api-host';

@Component({
  selector: 'app-dashboard',
  imports: [
    CommonModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  products: ProductModel[] = [];
  loading = false;
  errorMessage = '';
  currentUser: any = null;
  stats: SellerOrderStats | null = null;
  productStats: SellerProductStats | null = null;
  statsLoading = true;
  productStatsLoading = true;
  private readonly authApiBase = resolveApiBase(8081);

  constructor(
    private readonly productService: Product,
    private readonly authService: Auth,
    private readonly orderService: OrderService,
    private readonly mediaService: MediaService,
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadMyProducts();
    this.loadStats();
    this.loadProductStats();
  }

  loadStats(): void {
    this.statsLoading = true;
    this.orderService.getSellerStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.statsLoading = false;
      },
      error: () => {
        this.statsLoading = false;
      }
    });
  }

  loadProductStats(): void {
    this.productStatsLoading = true;
    this.orderService.getSellerProductStats().subscribe({
      next: (productStats) => {
        this.productStats = productStats;
        this.productStatsLoading = false;
      },
      error: () => {
        this.productStatsLoading = false;
      }
    });
  }

  getProductImageUrl(imageUrl: string | undefined): string {
    if (!imageUrl) return '';
    return this.mediaService.getImageUrl(imageUrl);
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(amount);
  }

  getAvatarUrl(avatar: string): string {
    return `${this.authApiBase}${avatar}`;
  }

  loadMyProducts(): void {
    this.loading = true;
    this.errorMessage = '';

    this.productService.getMyProducts().subscribe({
      next: (data) => {
        this.products = data;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur de chargement:', error);
        this.errorMessage = 'Impossible de charger vos produits';
        this.loading = false;
      }
    });
  }

  addProduct(): void {
    const dialogRef = this.dialog.open(ProductFormDialog, {
      width: '600px',
      data: { mode: 'create' }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.success) {
        this.snackBar.open('Produit créé avec succès!', 'Fermer', {
          duration: 3000,
          horizontalPosition: 'center',
          verticalPosition: 'top',
          panelClass: ['success-snackbar']
        });
        this.loadMyProducts();
      }
    });
  }

  editProduct(product: ProductModel): void {
    const dialogRef = this.dialog.open(ProductFormDialog, {
      width: '600px',
      data: { product, mode: 'edit' }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.success) {
        this.snackBar.open('Produit modifié avec succès!', 'Fermer', {
          duration: 3000,
          horizontalPosition: 'center',
          verticalPosition: 'top',
          panelClass: ['success-snackbar']
        });
        this.loadMyProducts();
      }
    });
  }

  deleteProduct(product: ProductModel): void {
    if (confirm(`Supprimer "${product.name}" ? Toutes les images seront aussi supprimées.`)) {
      this.productService.deleteProduct(product.id).subscribe({
        next: () => {
          this.snackBar.open('Produit supprimé avec succès!', 'Fermer', {
            duration: 3000,
            horizontalPosition: 'center',
            verticalPosition: 'top',
            panelClass: ['success-snackbar']
          });
          this.loadMyProducts();
        },
        error: (error) => {
          console.error('Erreur de suppression:', error);
          this.snackBar.open('Erreur lors de la suppression', 'Fermer', {
            duration: 3000,
            panelClass: ['error-snackbar']
          });
        }
      });
    }
  }

  viewProducts(): void {
    this.router.navigate(['/products']);
  }

  viewOrders(): void {
    this.router.navigate(['/seller/orders']);
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Product } from '../../../core/services/product';
import { Auth } from '../../../core/services/auth';
import { Product as ProductModel } from '../../../core/models/product.model';
import { ProductFormDialog } from '../product-form-dialog/product-form-dialog';
import { resolveApiBase } from '../../../core/utils/api-host';

@Component({
  selector: 'app-dashboard',
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  products: ProductModel[] = [];
  displayedColumns: string[] = ['name', 'category', 'price', 'stock', 'actions'];
  loading = false;
  errorMessage = '';
  currentUser: any = null;
  private readonly authApiBase = resolveApiBase(8081);

  constructor(
    private readonly productService: Product,
    private readonly authService: Auth,
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadMyProducts();
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
        console.log('Mes produits:', data);
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

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}

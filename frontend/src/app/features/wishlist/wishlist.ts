import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { WishlistService } from '../../core/services/wishlist';
import { Product } from '../../core/services/product';
import { MediaService } from '../../core/services/media';
import { Cart } from '../../core/services/cart';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError, map, switchMap, skip } from 'rxjs/operators';
import { PageHeader } from '../../shared/page-header/page-header';

@Component({
  selector: 'app-wishlist',
  standalone: true,
  imports: [
    CommonModule,
    MatSnackBarModule,
    PageHeader
  ],
  templateUrl: './wishlist.html',
  styleUrl: './wishlist.scss'
})
export class WishlistPage implements OnInit, OnDestroy {
  products: any[] = [];
  loading = false;
  errorMessage = '';
  private wishlistSubscription?: Subscription;

  constructor(
    private readonly wishlistService: WishlistService,
    private readonly productService: Product,
    private readonly mediaService: MediaService,
    private readonly cartService: Cart,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadWishlist();
  }

  ngOnDestroy(): void {
    this.wishlistSubscription?.unsubscribe();
  }

  loadWishlist(): void {
    this.loading = true;
    this.errorMessage = '';

    this.wishlistService.getWishlistIds().pipe(
      switchMap(productIds => {
        if (productIds.length === 0) {
          return of([]);
        }

        const productRequests = productIds.map(id =>
          this.productService.getProductById(id).pipe(
            switchMap(product =>
              this.mediaService.getMediaByProduct(product.id).pipe(
                map(media => ({
                  ...product,
                  imageUrl: media.length > 0 ? this.mediaService.getImageUrl(media[0].url) : null
                })),
                catchError(() => of({ ...product, imageUrl: null }))
              )
            ),
            catchError(() => of(null))
          )
        );

        return forkJoin(productRequests);
      })
    ).subscribe({
      next: (products) => {
        this.products = products.filter(p => p !== null);
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement wishlist:', error);
        this.errorMessage = 'Impossible de charger la wishlist';
        this.loading = false;
      }
    });
  }

  removeFromWishlist(product: any): void {
    this.wishlistService.removeFromWishlist(product.id).subscribe({
      next: () => {
        this.products = this.products.filter(p => p.id !== product.id);
        this.snackBar.open(`${product.name} retiré de la wishlist`, 'Fermer', {
          duration: 2000
        });
      },
      error: () => {
        this.snackBar.open('Erreur lors de la suppression', 'Fermer', {
          duration: 3000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  addToCart(product: any): void {
    if (product.stock === 0) {
      this.snackBar.open('Produit en rupture de stock', 'Fermer', {
        duration: 2000,
        panelClass: ['error-snackbar']
      });
      return;
    }

    const currentCart = this.cartService.getCartItems();
    const existingItem = currentCart.find(item => item.productId === product.id);
    const currentQuantityInCart = existingItem ? existingItem.quantity : 0;

    if (currentQuantityInCart >= product.stock) {
      this.snackBar.open(`Stock maximum atteint (${product.stock} disponible)`, 'Fermer', {
        duration: 3000,
        panelClass: ['error-snackbar']
      });
      return;
    }

    this.cartService.addToCart({
      productId: product.id,
      name: product.name,
      price: product.price,
      quantity: 1,
      imageUrl: product.imageUrl || null,
      stock: product.stock,
      sellerId: product.sellerId,
      sellerName: product.sellerName
    });

    this.snackBar.open(`${product.name} ajouté au panier`, 'Voir le panier', {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom',
      panelClass: ['success-snackbar']
    }).onAction().subscribe(() => {
      this.router.navigate(['/cart']);
    });
  }

  viewDetails(productId: string): void {
    this.router.navigate(['/products', productId]);
  }

  clearWishlist(): void {
    if (confirm('Vider toute la wishlist ?')) {
      this.wishlistService.clearWishlist().subscribe({
        next: () => {
          this.products = [];
          this.snackBar.open('Wishlist vidée', 'Fermer', { duration: 2000 });
        },
        error: () => {
          this.snackBar.open('Erreur lors de la suppression', 'Fermer', {
            duration: 3000,
            panelClass: ['error-snackbar']
          });
        }
      });
    }
  }

  getStockBadgeClass(product: any): string {
    if (product.stock === 0) return 'bg-error-50 text-error-700';
    if (product.stock < 10) return 'bg-warning-50 text-warning-700';
    return 'bg-success-50 text-success-700';
  }

  goBack(): void {
    this.router.navigate(['/products']);
  }

  goToCart(): void {
    this.router.navigate(['/cart']);
  }
}
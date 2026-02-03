import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Product as ProductModel } from '../../../core/models/product.model';
import { Product } from '../../../core/services/product';
import { MediaService } from '../../../core/services/media';
import { Cart } from '../../../core/services/cart';
import { WishlistService } from '../../../core/services/wishlist';
import { ProductReviews } from '../product-reviews/product-reviews';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, MatSnackBarModule, ProductReviews],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.scss'
})
export class ProductDetail implements OnInit {
  product: ProductModel | null = null;
  images: string[] = [];
  selectedImageIndex = 0;
  loading = true;
  errorMessage = '';
  quantity = 1;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly productService: Product,
    private readonly mediaService: MediaService,
    private readonly cartService: Cart,
    private readonly wishlistService: WishlistService,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const productId = this.route.snapshot.paramMap.get('id');
    if (productId) {
      this.loadProduct(productId);
    } else {
      this.errorMessage = 'ID produit manquant';
      this.loading = false;
    }
  }

  loadProduct(id: string): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      product: this.productService.getProductById(id),
      media: this.mediaService.getMediaByProduct(id).pipe(
        catchError(() => of([]))
      )
    }).subscribe({
      next: (result) => {
        this.product = result.product;
        this.images = result.media.map(m => this.mediaService.getImageUrl(m.url));
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement produit:', error);
        this.errorMessage = 'Impossible de charger le produit';
        this.loading = false;
      }
    });
  }

  selectImage(index: number): void {
    this.selectedImageIndex = index;
  }

  previousImage(): void {
    if (this.selectedImageIndex > 0) {
      this.selectedImageIndex--;
    }
  }

  nextImage(): void {
    if (this.selectedImageIndex < this.images.length - 1) {
      this.selectedImageIndex++;
    }
  }

  increaseQuantity(): void {
    if (this.product && this.quantity < this.product.stock) {
      this.quantity++;
    }
  }

  decreaseQuantity(): void {
    if (this.quantity > 1) {
      this.quantity--;
    }
  }

  addToCart(): void {
    const product = this.product;
    if (!product) return;

    const currentCart = this.cartService.getCartItems();
    const existingItem = currentCart.find(item => item.productId === product.id);
    const currentQuantityInCart = existingItem ? existingItem.quantity : 0;

    if (currentQuantityInCart + this.quantity > product.stock) {
      const remaining = product.stock - currentQuantityInCart;
      this.snackBar.open(
        remaining > 0
          ? `Stock insuffisant ! Seulement ${remaining} disponible(s) en plus`
          : `Stock maximum déjà atteint (${product.stock} en stock)`,
        'Fermer',
        { duration: 4000, panelClass: ['error-snackbar'] }
      );
      return;
    }

    this.cartService.addToCart({
      productId: product.id,
      name: product.name,
      price: product.price,
      quantity: this.quantity,
      imageUrl: this.images[0] || null,
      stock: product.stock,
      sellerId: product.sellerId,
      sellerName: product.sellerName
    });

    this.snackBar.open(`${this.quantity} x ${product.name} ajouté au panier`, 'Voir le panier', {
      duration: 3000, panelClass: ['success-snackbar']
    }).onAction().subscribe(() => {
      this.router.navigate(['/cart']);
    });

    this.quantity = 1;
  }

  goBack(): void {
    this.router.navigate(['/products']);
  }

  get stockStatus(): string {
    if (!this.product) return '';
    if (this.product.stock === 0) return 'Rupture de stock';
    if (this.product.stock < 10) return 'Stock limité';
    return 'En stock';
  }

  get stockColorClass(): string {
    if (!this.product) return '';
    if (this.product.stock === 0) return 'bg-error-50 text-error-700';
    if (this.product.stock < 10) return 'bg-warning-50 text-warning-700';
    return 'bg-success-50 text-success-700';
  }

  isInWishlist(): boolean {
    return this.product ? this.wishlistService.isInWishlistSync(this.product.id) : false;
  }

  toggleWishlist(): void {
    if (!this.product) return;

    this.wishlistService.toggleWishlist(this.product.id).subscribe({
      next: () => {
        const action = this.isInWishlist() ? 'ajouté à' : 'retiré de';
        this.snackBar.open(`${this.product!.name} ${action} la wishlist`, 'Fermer', { duration: 2000 });
      },
      error: () => {
        this.snackBar.open('Erreur lors de la mise à jour de la wishlist', 'Fermer', {
          duration: 3000, panelClass: ['error-snackbar']
        });
      }
    });
  }
}
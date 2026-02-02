import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatSliderModule } from '@angular/material/slider';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { Product } from '../../../core/services/product';
import { MediaService } from '../../../core/services/media';
import { Cart } from '../../../core/services/cart';
import { Auth } from '../../../core/services/auth';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

@Component({
  selector: 'app-product-list',
  imports: [
    CommonModule,
    FormsModule,
    MatToolbarModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatSelectModule,
    MatSliderModule,
    MatChipsModule,
    MatExpansionModule
  ],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList implements OnInit {
  products: any[] = []; // Produits avec leurs images
  loading = false;
  errorMessage = '';
  searchKeyword = '';
  cartCount = 0;

  // Filtres
  selectedCategory = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  showFilters = false;

  constructor(
    private readonly productService: Product,
    private readonly mediaService: MediaService,
    private readonly cartService: Cart,
    private readonly authService: Auth,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadProducts();
    this.updateCartCount();
    this.cartService.cartItems$.subscribe(() => {
      this.updateCartCount();
    });
  }

  loadProducts(): void {
    this.loading = true;
    this.errorMessage = '';

    this.productService.getAllProducts().subscribe({
      next: (products) => {
        // Pour chaque produit, charger ses images
        const productsWithImages$ = products.map(product => 
          this.mediaService.getMediaByProduct(product.id).pipe(
            map(media => ({
              ...product,
              imageUrl: media.length > 0 ? this.mediaService.getImageUrl(media[0].url) : null
            })),
            catchError(() => of({ ...product, imageUrl: null }))
          )
        );

        // Attendre que toutes les images soient chargées
        if (productsWithImages$.length > 0) {
          forkJoin(productsWithImages$).subscribe({
            next: (productsWithImages) => {
              this.products = productsWithImages;
              this.loading = false;
              console.log('Produits avec images:', productsWithImages);
            },
            error: (error) => {
              console.error('Erreur chargement images:', error);
              this.products = products;
              this.loading = false;
            }
          });
        } else {
          this.products = [];
          this.loading = false;
        }
      },
      error: (error) => {
        console.error('Erreur de chargement des produits:', error);
        this.errorMessage = 'Impossible de charger les produits. Vérifiez que le backend est démarré.';
        this.loading = false;
      }
    });
  }

  onSearch(): void {
    if (!this.searchKeyword.trim()) {
      this.loadProducts();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.productService.searchProducts(this.searchKeyword).subscribe({
      next: (products) => {
        // Charger les images pour les résultats de recherche
        const productsWithImages$ = products.map(product => 
          this.mediaService.getMediaByProduct(product.id).pipe(
            map(media => ({
              ...product,
              imageUrl: media.length > 0 ? this.mediaService.getImageUrl(media[0].url) : null
            })),
            catchError(() => of({ ...product, imageUrl: null }))
          )
        );

        if (productsWithImages$.length > 0) {
          forkJoin(productsWithImages$).subscribe({
            next: (productsWithImages) => {
              this.products = productsWithImages;
              this.loading = false;
            },
            error: (error) => {
              console.error('Erreur chargement images:', error);
              this.products = products;
              this.loading = false;
            }
          });
        } else {
          this.products = [];
          this.loading = false;
        }
      },
      error: (error) => {
        console.error('Erreur de recherche:', error);
        this.errorMessage = 'Erreur lors de la recherche';
        this.loading = false;
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  viewDetails(productId: string): void {
    this.router.navigate(['/products', productId]);
  }

  updateCartCount(): void {
    this.cartCount = this.cartService.getCartCount();
  }

  addToCart(product: any): void {
    if (product.stock === 0) {
      this.snackBar.open('Produit en rupture de stock', 'Fermer', {
        duration: 2000,
        panelClass: ['error-snackbar']
      });
      return;
    }

    // Vérifier si le panier ne dépasse pas le stock disponible
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

  goToCart(): void {
    this.router.navigate(['/cart']);
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  // Filtres
  get categories(): string[] {
    const cats = this.products.map(p => p.category).filter(c => c);
    return [...new Set(cats)].sort();
  }

  get filteredProducts(): any[] {
    return this.products.filter(product => {
      // Filtre par catégorie
      if (this.selectedCategory && product.category !== this.selectedCategory) {
        return false;
      }
      // Filtre par prix minimum
      if (this.minPrice !== null && product.price < this.minPrice) {
        return false;
      }
      // Filtre par prix maximum
      if (this.maxPrice !== null && product.price > this.maxPrice) {
        return false;
      }
      return true;
    });
  }

  get priceRange(): { min: number; max: number } {
    if (this.products.length === 0) return { min: 0, max: 1000 };
    const prices = this.products.map(p => p.price);
    return {
      min: Math.floor(Math.min(...prices)),
      max: Math.ceil(Math.max(...prices))
    };
  }

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  clearFilters(): void {
    this.selectedCategory = '';
    this.minPrice = null;
    this.maxPrice = null;
  }

  hasActiveFilters(): boolean {
    return this.selectedCategory !== '' || this.minPrice !== null || this.maxPrice !== null;
  }
}

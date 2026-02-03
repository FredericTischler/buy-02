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
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { Product } from '../../../core/services/product';
import { MediaService } from '../../../core/services/media';
import { Cart } from '../../../core/services/cart';
import { Auth } from '../../../core/services/auth';
import { WishlistService } from '../../../core/services/wishlist';
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
    MatExpansionModule,
    MatPaginatorModule
  ],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList implements OnInit {
  products: any[] = [];
  loading = false;
  errorMessage = '';
  searchKeyword = '';
  cartCount = 0;
  wishlistCount = 0;

  // Filtres
  selectedCategory = '';
  availableCategories: string[] = [];
  minPrice: number | null = null;
  maxPrice: number | null = null;
  showFilters = false;

  // Tri
  sortBy: 'price-asc' | 'price-desc' | 'name-asc' | 'name-desc' | 'newest' | 'stock' = 'newest';
  sortOptions = [
    { value: 'newest', label: 'Plus récents' },
    { value: 'price-asc', label: 'Prix croissant' },
    { value: 'price-desc', label: 'Prix décroissant' },
    { value: 'name-asc', label: 'Nom A-Z' },
    { value: 'name-desc', label: 'Nom Z-A' },
    { value: 'stock', label: 'Disponibilité' }
  ];

  // Pagination
  pageIndex = 0;
  pageSize = 12;
  totalElements = 0;
  pageSizeOptions = [12, 24, 48];

  constructor(
    private readonly productService: Product,
    private readonly mediaService: MediaService,
    private readonly cartService: Cart,
    private readonly authService: Auth,
    private readonly wishlistService: WishlistService,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
    this.updateCartCount();
    this.updateWishlistCount();
    this.cartService.cartItems$.subscribe(() => {
      this.updateCartCount();
    });
    this.wishlistService.wishlist$.subscribe(() => {
      this.updateWishlistCount();
    });
  }

  loadCategories(): void {
    this.productService.getAllProducts().subscribe({
      next: (products) => {
        const cats = products.map(p => p.category).filter(c => c);
        this.availableCategories = [...new Set(cats)].sort();
      }
    });
  }

  private getSortParams(): { sortBy: string; sortDir: string } {
    switch (this.sortBy) {
      case 'price-asc': return { sortBy: 'price', sortDir: 'asc' };
      case 'price-desc': return { sortBy: 'price', sortDir: 'desc' };
      case 'name-asc': return { sortBy: 'name', sortDir: 'asc' };
      case 'name-desc': return { sortBy: 'name', sortDir: 'desc' };
      case 'stock': return { sortBy: 'stock', sortDir: 'desc' };
      case 'newest':
      default: return { sortBy: 'createdAt', sortDir: 'desc' };
    }
  }

  loadProducts(): void {
    this.loading = true;
    this.errorMessage = '';

    const sort = this.getSortParams();

    this.productService.filterProducts({
      keyword: this.searchKeyword.trim() || undefined,
      category: this.selectedCategory || undefined,
      minPrice: this.minPrice ?? undefined,
      maxPrice: this.maxPrice ?? undefined,
      page: this.pageIndex,
      size: this.pageSize,
      sortBy: sort.sortBy,
      sortDir: sort.sortDir
    }).subscribe({
      next: (page) => {
        this.totalElements = page.totalElements;

        const products = page.content;
        if (products.length === 0) {
          this.products = [];
          this.loading = false;
          return;
        }

        const productsWithImages$ = products.map(product =>
          this.mediaService.getMediaByProduct(product.id).pipe(
            map(media => ({
              ...product,
              imageUrl: media.length > 0 ? this.mediaService.getImageUrl(media[0].url) : null
            })),
            catchError(() => of({ ...product, imageUrl: null }))
          )
        );

        forkJoin(productsWithImages$).subscribe({
          next: (productsWithImages) => {
            this.products = productsWithImages;
            this.loading = false;
          },
          error: () => {
            this.products = products;
            this.loading = false;
          }
        });
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les produits. Vérifiez que le backend est démarré.';
        this.loading = false;
      }
    });
  }

  onSearch(): void {
    this.pageIndex = 0;
    this.loadProducts();
  }

  onSortChange(): void {
    this.pageIndex = 0;
    this.loadProducts();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadProducts();
  }

  applyFilters(): void {
    this.pageIndex = 0;
    this.loadProducts();
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

  updateWishlistCount(): void {
    this.wishlistCount = this.wishlistService.getWishlistCount();
  }

  isInWishlist(productId: string): boolean {
    return this.wishlistService.isInWishlistSync(productId);
  }

  toggleWishlist(product: any, event: Event): void {
    event.stopPropagation();
    this.wishlistService.toggleWishlist(product.id).subscribe({
      next: () => {
        const action = this.isInWishlist(product.id) ? 'ajouté à' : 'retiré de';
        this.snackBar.open(`${product.name} ${action} la wishlist`, 'Fermer', {
          duration: 2000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom'
        });
      },
      error: () => {
        this.snackBar.open('Erreur lors de la mise à jour de la wishlist', 'Fermer', {
          duration: 3000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  goToWishlist(): void {
    this.router.navigate(['/wishlist']);
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

  goToCart(): void {
    this.router.navigate(['/cart']);
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  // Filtres
  get categories(): string[] {
    return this.availableCategories;
  }

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  clearFilters(): void {
    this.selectedCategory = '';
    this.minPrice = null;
    this.maxPrice = null;
    this.searchKeyword = '';
    this.pageIndex = 0;
    this.loadProducts();
  }

  hasActiveFilters(): boolean {
    return this.selectedCategory !== '' || this.minPrice !== null || this.maxPrice !== null;
  }
}
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Product as ProductService } from '../../../core/services/product';
import { MediaService } from '../../../core/services/media';
import { Cart } from '../../../core/services/cart';
import { WishlistService } from '../../../core/services/wishlist';
import { Product } from '../../../core/models/product.model';
import { Navbar } from '../../../shared/navbar/navbar';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, FormsModule, MatSnackBarModule, Navbar],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList implements OnInit {
  products: Product[] = [];
  loading = false;
  errorMessage = '';
  searchKeyword = '';
  showFilters = false;
  selectedCategory = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  sortBy = 'name';
  wishlistCount = 0;
  wishlistProductIds: Set<string> = new Set();

  // Pagination
  pageIndex = 0;
  pageSize = 12;
  pageSizeOptions = [6, 12, 24, 48];
  totalElements = 0;

  categories: string[] = [];
  sortOptions = [
    { value: 'name', label: 'Nom (A-Z)' },
    { value: 'name,desc', label: 'Nom (Z-A)' },
    { value: 'price', label: 'Prix croissant' },
    { value: 'price,desc', label: 'Prix décroissant' },
    { value: 'createdAt,desc', label: 'Plus récents' }
  ];

  constructor(
    private readonly productService: ProductService,
    private readonly mediaService: MediaService,
    private readonly cartService: Cart,
    private readonly wishlistService: WishlistService,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadProducts();
    this.loadCategories();
    this.loadWishlist();
  }

  loadProducts(): void {
    this.loading = true;
    this.errorMessage = '';

    this.productService.getProducts(
      this.pageIndex,
      this.pageSize,
      this.sortBy,
      this.searchKeyword || undefined,
      this.selectedCategory || undefined,
      this.minPrice ?? undefined,
      this.maxPrice ?? undefined
    ).subscribe({
      next: (response: any) => {
        this.products = response.content || response;
        this.totalElements = response.totalElements || this.products.length;
        this.products.forEach(p => {
          if (p.imageUrl) {
            p.imageUrl = this.mediaService.getImageUrl(p.imageUrl);
          }
        });
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.errorMessage = 'Impossible de charger les produits';
        this.loading = false;
      }
    });
  }

  loadCategories(): void {
    this.productService.getCategories().subscribe({
      next: (cats) => this.categories = cats,
      error: () => {}
    });
  }

  loadWishlist(): void {
    this.wishlistService.getWishlist().subscribe({
      next: (items) => {
        this.wishlistProductIds = new Set(items.map((i: any) => i.productId));
        this.wishlistCount = items.length;
      },
      error: () => {}
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

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  applyFilters(): void {
    this.pageIndex = 0;
    this.loadProducts();
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
    return !!(this.selectedCategory || this.minPrice || this.maxPrice);
  }

  addToCart(product: Product): void {
    this.cartService.addToCart({
      productId: product.id,
      name: product.name,
      price: product.price,
      quantity: 1,
      imageUrl: product.imageUrl ?? null,
      sellerId: product.sellerId,
      sellerName: product.sellerName,
      stock: product.stock
    });
    this.snackBar.open(`${product.name} ajouté au panier`, 'Fermer', {
      duration: 2000,
      panelClass: ['success-snackbar']
    });
  }

  toggleWishlist(product: Product, event: Event): void {
    event.stopPropagation();
    if (this.isInWishlist(product.id)) {
      this.wishlistService.removeFromWishlist(product.id).subscribe({
        next: () => {
          this.wishlistProductIds.delete(product.id);
          this.wishlistCount--;
          this.snackBar.open('Retiré de la wishlist', 'Fermer', { duration: 2000 });
        }
      });
    } else {
      this.wishlistService.addToWishlist(product.id).subscribe({
        next: () => {
          this.wishlistProductIds.add(product.id);
          this.wishlistCount++;
          this.snackBar.open('Ajouté à la wishlist', 'Fermer', { duration: 2000 });
        }
      });
    }
  }

  isInWishlist(productId: string): boolean {
    return this.wishlistProductIds.has(productId);
  }

  viewDetails(productId: string): void {
    this.router.navigate(['/products', productId]);
  }

  onSearchFromNavbar(keyword: string): void {
    this.searchKeyword = keyword;
    this.onSearch();
  }

  onPageChange(event: { pageIndex: number; pageSize: number }): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadProducts();
  }

  get totalPages(): number {
    return Math.ceil(this.totalElements / this.pageSize);
  }

  get pages(): number[] {
    const total = this.totalPages;
    const current = this.pageIndex;
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(0, current - Math.floor(maxVisible / 2));
    let end = Math.min(total, start + maxVisible);
    if (end - start < maxVisible) {
      start = Math.max(0, end - maxVisible);
    }
    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    return pages;
  }
}
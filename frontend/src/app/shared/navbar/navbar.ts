import { Component, OnInit, OnDestroy, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { Auth } from '../../core/services/auth';
import { Cart } from '../../core/services/cart';
import { WishlistService } from '../../core/services/wishlist';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss'
})
export class Navbar implements OnInit, OnDestroy {
  @Input() showSearch = true;
  @Output() search = new EventEmitter<string>();

  searchKeyword = '';
  cartCount = 0;
  wishlistCount = 0;
  isSeller = false;

  private subscriptions: Subscription[] = [];

  constructor(
    private readonly router: Router,
    private readonly authService: Auth,
    private readonly cartService: Cart,
    private readonly wishlistService: WishlistService
  ) {}

  ngOnInit(): void {
    this.isSeller = this.authService.isSeller();

    this.subscriptions.push(
      this.cartService.cartItems$.subscribe(items => {
        this.cartCount = items.reduce((sum, item) => sum + item.quantity, 0);
      })
    );

    this.subscriptions.push(
      this.wishlistService.getWishlist().subscribe({
        next: (items) => this.wishlistCount = items.length,
        error: () => {}
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  onSearch(): void {
    this.search.emit(this.searchKeyword);
  }

  goToProducts(): void {
    this.router.navigate(['/products']);
  }

  goToWishlist(): void {
    this.router.navigate(['/wishlist']);
  }

  goToCart(): void {
    this.router.navigate(['/cart']);
  }

  goToOrders(): void {
    this.router.navigate(['/orders']);
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  goToSellerDashboard(): void {
    this.router.navigate(['/seller/dashboard']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
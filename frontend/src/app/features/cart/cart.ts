import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Cart } from '../../core/services/cart';
import { CartItem } from '../../core/models/cart.model';
import { PageHeader } from '../../shared/page-header/page-header';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, MatSnackBarModule, PageHeader],
  templateUrl: './cart.html',
  styleUrl: './cart.scss'
})
export class CartPage implements OnInit {
  cartItems: CartItem[] = [];

  constructor(
    private readonly cartService: Cart,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadCart();
    this.cartService.cartItems$.subscribe((items: CartItem[]) => {
      this.cartItems = items;
    });
  }

  loadCart(): void {
    this.cartItems = this.cartService.getCartItems();
  }

  increaseQuantity(item: CartItem): void {
    const stock = item.stock ?? Infinity;
    if (item.quantity < stock) {
      this.cartService.updateQuantity(item.productId, item.quantity + 1);
    } else {
      this.snackBar.open(`Stock maximum atteint (${stock} disponibles)`, 'Fermer', {
        duration: 3000, panelClass: ['error-snackbar']
      });
    }
  }

  decreaseQuantity(item: CartItem): void {
    if (item.quantity > 1) {
      this.cartService.updateQuantity(item.productId, item.quantity - 1);
    }
  }

  removeItem(item: CartItem): void {
    this.cartService.removeFromCart(item.productId);
    this.snackBar.open(`${item.name} retiré du panier`, 'Fermer', {
      duration: 2000, panelClass: ['success-snackbar']
    });
  }

  clearCart(): void {
    if (confirm('Vider tout le panier ?')) {
      this.cartService.clearCart();
      this.snackBar.open('Panier vidé', 'Fermer', { duration: 2000 });
    }
  }

  getTotal(): number { return this.cartService.getCartTotal(); }
  getItemTotal(item: CartItem): number { return item.price * item.quantity; }
  continueShopping(): void { this.router.navigate(['/products']); }

  checkout(): void {
    if (this.cartItems.length === 0) {
      this.snackBar.open('Votre panier est vide', 'Fermer', { duration: 3000 });
      return;
    }
    this.router.navigate(['/checkout']);
  }

  goBack(): void { this.router.navigate(['/products']); }
}
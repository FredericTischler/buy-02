import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Cart } from '../../core/services/cart';
import { CartItem } from '../../core/models/cart.model';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatToolbarModule,
    MatDividerModule,
    MatTooltipModule,
    MatSnackBarModule
  ],
  templateUrl: './cart.html',
  styleUrl: './cart.scss'
})
export class CartPage implements OnInit {
  cartItems: CartItem[] = [];
  displayedColumns: string[] = ['image', 'name', 'price', 'quantity', 'total', 'actions'];

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
    // Vérifier si on peut augmenter la quantité (stock disponible)
    const stock = item.stock ?? Infinity; // Si stock undefined, pas de limite
    if (item.quantity < stock) {
      this.cartService.updateQuantity(item.productId, item.quantity + 1);
    } else {
      this.snackBar.open(`Stock maximum atteint (${stock} disponibles)`, 'Fermer', {
        duration: 3000,
        panelClass: ['error-snackbar']
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
      duration: 2000,
      panelClass: ['success-snackbar']
    });
  }

  clearCart(): void {
    if (confirm('Vider tout le panier ?')) {
      this.cartService.clearCart();
      this.snackBar.open('Panier vidé', 'Fermer', {
        duration: 2000
      });
    }
  }

  getTotal(): number {
    return this.cartService.getCartTotal();
  }

  getItemTotal(item: CartItem): number {
    return item.price * item.quantity;
  }

  continueShopping(): void {
    this.router.navigate(['/products']);
  }

  checkout(): void {
    // TODO: Implémenter le système de commande
    this.snackBar.open('Fonction de commande à venir!', 'OK', {
      duration: 3000
    });
  }

  goBack(): void {
    this.router.navigate(['/products']);
  }
}

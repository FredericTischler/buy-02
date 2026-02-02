import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { CartItem } from '../models/cart.model';

@Injectable({
  providedIn: 'root',
})
export class Cart {
  private readonly cartItemsSubject = new BehaviorSubject<CartItem[]>(this.getCartFromStorage());
  public cartItems$: Observable<CartItem[]> = this.cartItemsSubject.asObservable();

  constructor() {}

  private getCurrentUserId(): string | null {
    const user = localStorage.getItem('current_user');
    if (user) {
      const userData = JSON.parse(user);
      return userData.id || userData.email; // Utiliser l'ID ou l'email comme identifiant unique
    }
    return null;
  }

  private getCartKey(): string {
    const userId = this.getCurrentUserId();
    return userId ? `cart_${userId}` : 'cart'; // Clé unique par utilisateur
  }

  private getCartFromStorage(): CartItem[] {
    const cartKey = this.getCartKey();
    const cart = localStorage.getItem(cartKey);
    return cart ? JSON.parse(cart) : [];
  }

  private saveCartToStorage(items: CartItem[]): void {
    const cartKey = this.getCartKey();
    localStorage.setItem(cartKey, JSON.stringify(items));
    this.cartItemsSubject.next(items);
  }

  // Méthode pour recharger le panier après connexion
  public loadCart(): void {
    const cart = this.getCartFromStorage();
    this.cartItemsSubject.next(cart);
  }

  // Méthode pour vider le panier lors de la déconnexion
  public clearCartOnLogout(): void {
    this.cartItemsSubject.next([]);
  }

  getCartItems(): CartItem[] {
    return this.cartItemsSubject.value;
  }

  getCartCount(): number {
    return this.cartItemsSubject.value.reduce((total, item) => total + item.quantity, 0);
  }

  getCartTotal(): number {
    return this.cartItemsSubject.value.reduce((total, item) => total + (item.price * item.quantity), 0);
  }

  addToCart(item: CartItem): void {
    const currentCart = this.getCartItems();
    const existingItemIndex = currentCart.findIndex(i => i.productId === item.productId);

    if (existingItemIndex > -1) {
      currentCart[existingItemIndex].quantity += item.quantity;
    } else {
      currentCart.push(item);
    }

    this.saveCartToStorage(currentCart);
  }

  updateQuantity(productId: string, quantity: number): void {
    const currentCart = this.getCartItems();
    const itemIndex = currentCart.findIndex(i => i.productId === productId);

    if (itemIndex > -1) {
      if (quantity <= 0) {
        currentCart.splice(itemIndex, 1);
      } else {
        currentCart[itemIndex].quantity = quantity;
      }
      this.saveCartToStorage(currentCart);
    }
  }

  removeFromCart(productId: string): void {
    const currentCart = this.getCartItems();
    const updatedCart = currentCart.filter(item => item.productId !== productId);
    this.saveCartToStorage(updatedCart);
  }

  clearCart(): void {
    this.saveCartToStorage([]);
  }
}

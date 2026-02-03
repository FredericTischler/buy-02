import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { CartItem } from '../models/cart.model';
import { resolveApiBase } from '../utils/api-host';

interface CartResponse {
  id: string;
  userId: string;
  items: CartItem[];
  totalAmount: number;
  totalItems: number;
  createdAt: string;
  updatedAt: string;
}

interface CartItemRequest {
  productId: string;
  productName: string;
  sellerId: string;
  sellerName: string;
  price: number;
  quantity: number;
  stock: number;
  imageUrl: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class Cart {
  private readonly API_URL = `${resolveApiBase(8085)}/api/cart`;
  private readonly cartItemsSubject = new BehaviorSubject<CartItem[]>([]);
  public cartItems$: Observable<CartItem[]> = this.cartItemsSubject.asObservable();
  private syncInProgress = false;

  constructor(private http: HttpClient) {}

  private getCurrentUserId(): string | null {
    const user = localStorage.getItem('current_user');
    if (user) {
      const userData = JSON.parse(user);
      return userData.id || userData.email;
    }
    return null;
  }

  private getCartKey(): string {
    const userId = this.getCurrentUserId();
    return userId ? `cart_${userId}` : 'cart';
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

  private mapResponseToItems(response: CartResponse): CartItem[] {
    return response.items.map(item => ({
      productId: item.productId,
      name: item.productName || (item as any).name,
      price: item.price,
      quantity: item.quantity,
      imageUrl: item.imageUrl,
      stock: item.stock,
      sellerId: item.sellerId,
      sellerName: item.sellerName
    }));
  }

  private toCartItemRequest(item: CartItem): CartItemRequest {
    return {
      productId: item.productId,
      productName: item.name,
      sellerId: item.sellerId,
      sellerName: item.sellerName,
      price: item.price,
      quantity: item.quantity,
      stock: item.stock || 0,
      imageUrl: item.imageUrl
    };
  }

  // Load cart from backend on login
  public loadCart(): void {
    const userId = this.getCurrentUserId();
    if (!userId) {
      const localCart = this.getCartFromStorage();
      this.cartItemsSubject.next(localCart);
      return;
    }

    this.http.get<CartResponse>(this.API_URL).pipe(
      tap(response => {
        const items = this.mapResponseToItems(response);
        this.saveCartToStorage(items);
      }),
      catchError(() => {
        // Fallback to local storage if backend fails
        const localCart = this.getCartFromStorage();
        this.cartItemsSubject.next(localCart);
        return of(null);
      })
    ).subscribe();
  }

  // Sync local cart to backend
  public syncCartToBackend(): void {
    if (this.syncInProgress) return;
    const userId = this.getCurrentUserId();
    if (!userId) return;

    this.syncInProgress = true;
    const items = this.getCartItems();
    const requests = items.map(item => this.toCartItemRequest(item));

    this.http.post<CartResponse>(`${this.API_URL}/sync`, requests).pipe(
      tap(response => {
        const syncedItems = this.mapResponseToItems(response);
        this.saveCartToStorage(syncedItems);
        this.syncInProgress = false;
      }),
      catchError(() => {
        this.syncInProgress = false;
        return of(null);
      })
    ).subscribe();
  }

  // Clear cart on logout (local only, backend cart persists)
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
    // Update local state immediately for responsiveness
    const currentCart = [...this.getCartItems()];
    const existingItemIndex = currentCart.findIndex(i => i.productId === item.productId);

    if (existingItemIndex > -1) {
      currentCart[existingItemIndex] = {
        ...currentCart[existingItemIndex],
        quantity: currentCart[existingItemIndex].quantity + item.quantity
      };
    } else {
      currentCart.push(item);
    }

    this.saveCartToStorage(currentCart);

    // Sync to backend
    const userId = this.getCurrentUserId();
    if (userId) {
      const request = this.toCartItemRequest(item);
      this.http.post<CartResponse>(`${this.API_URL}/items`, request).pipe(
        tap(response => {
          const items = this.mapResponseToItems(response);
          this.saveCartToStorage(items);
        }),
        catchError(() => of(null))
      ).subscribe();
    }
  }

  updateQuantity(productId: string, quantity: number): void {
    // Update local state immediately
    const currentCart = [...this.getCartItems()];
    const itemIndex = currentCart.findIndex(i => i.productId === productId);

    if (itemIndex > -1) {
      if (quantity <= 0) {
        currentCart.splice(itemIndex, 1);
      } else {
        currentCart[itemIndex] = { ...currentCart[itemIndex], quantity };
      }
      this.saveCartToStorage(currentCart);
    }

    // Sync to backend
    const userId = this.getCurrentUserId();
    if (userId) {
      if (quantity <= 0) {
        this.http.delete<CartResponse>(`${this.API_URL}/items/${productId}`).pipe(
          tap(response => {
            const items = this.mapResponseToItems(response);
            this.saveCartToStorage(items);
          }),
          catchError(() => of(null))
        ).subscribe();
      } else {
        this.http.put<CartResponse>(`${this.API_URL}/items/${productId}`, { quantity }).pipe(
          tap(response => {
            const items = this.mapResponseToItems(response);
            this.saveCartToStorage(items);
          }),
          catchError(() => of(null))
        ).subscribe();
      }
    }
  }

  removeFromCart(productId: string): void {
    // Update local state immediately
    const currentCart = this.getCartItems().filter(item => item.productId !== productId);
    this.saveCartToStorage(currentCart);

    // Sync to backend
    const userId = this.getCurrentUserId();
    if (userId) {
      this.http.delete<CartResponse>(`${this.API_URL}/items/${productId}`).pipe(
        tap(response => {
          const items = this.mapResponseToItems(response);
          this.saveCartToStorage(items);
        }),
        catchError(() => of(null))
      ).subscribe();
    }
  }

  clearCart(): void {
    // Update local state immediately
    this.saveCartToStorage([]);

    // Sync to backend
    const userId = this.getCurrentUserId();
    if (userId) {
      this.http.delete<CartResponse>(this.API_URL).pipe(
        catchError(() => of(null))
      ).subscribe();
    }
  }
}
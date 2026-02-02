import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, catchError, of } from 'rxjs';
import { resolveApiBase } from '../utils/api-host';

@Injectable({
  providedIn: 'root',
})
export class WishlistService {
  private readonly apiBase = resolveApiBase(8081);
  private readonly API_URL = `${this.apiBase}/api/users/wishlist`;

  private readonly wishlistSubject = new BehaviorSubject<string[]>([]);
  public wishlist$ = this.wishlistSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  /**
   * Charger la wishlist depuis le backend
   */
  loadWishlist(): void {
    this.http.get<string[]>(this.API_URL).pipe(
      catchError(() => of([]))
    ).subscribe(wishlist => {
      this.wishlistSubject.next(wishlist);
    });
  }

  /**
   * Récupérer la wishlist (met à jour le subject)
   */
  getWishlist(): Observable<string[]> {
    return this.http.get<string[]>(this.API_URL).pipe(
      tap(wishlist => this.wishlistSubject.next(wishlist))
    );
  }

  /**
   * Récupérer les IDs de la wishlist (sans mettre à jour le subject)
   */
  getWishlistIds(): Observable<string[]> {
    return this.http.get<string[]>(this.API_URL);
  }

  /**
   * Ajouter un produit à la wishlist
   */
  addToWishlist(productId: string): Observable<{ message: string; wishlist: string[] }> {
    return this.http.post<{ message: string; wishlist: string[] }>(`${this.API_URL}/${productId}`, {}).pipe(
      tap(response => this.wishlistSubject.next(response.wishlist))
    );
  }

  /**
   * Supprimer un produit de la wishlist
   */
  removeFromWishlist(productId: string): Observable<{ message: string; wishlist: string[] }> {
    return this.http.delete<{ message: string; wishlist: string[] }>(`${this.API_URL}/${productId}`).pipe(
      tap(response => this.wishlistSubject.next(response.wishlist))
    );
  }

  /**
   * Vérifier si un produit est dans la wishlist
   */
  isInWishlist(productId: string): Observable<{ inWishlist: boolean }> {
    return this.http.get<{ inWishlist: boolean }>(`${this.API_URL}/check/${productId}`);
  }

  /**
   * Vérifier localement si un produit est dans la wishlist
   */
  isInWishlistSync(productId: string): boolean {
    return this.wishlistSubject.value.includes(productId);
  }

  /**
   * Basculer l'état wishlist (ajouter/supprimer)
   */
  toggleWishlist(productId: string): Observable<{ message: string; wishlist: string[] }> {
    if (this.isInWishlistSync(productId)) {
      return this.removeFromWishlist(productId);
    } else {
      return this.addToWishlist(productId);
    }
  }

  /**
   * Vider la wishlist
   */
  clearWishlist(): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(this.API_URL).pipe(
      tap(() => this.wishlistSubject.next([]))
    );
  }

  /**
   * Récupérer le nombre d'éléments dans la wishlist
   */
  getWishlistCount(): number {
    return this.wishlistSubject.value.length;
  }

  /**
   * Réinitialiser la wishlist locale (à la déconnexion)
   */
  resetWishlist(): void {
    this.wishlistSubject.next([]);
  }
}
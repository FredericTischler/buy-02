import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Review, ReviewRequest, ProductReviewStats } from '../models/review.model';
import { resolveApiBase } from '../utils/api-host';

@Injectable({
  providedIn: 'root',
})
export class ReviewService {
  private readonly API_URL = `${resolveApiBase(8082)}/api/reviews`;

  constructor(private http: HttpClient) {}

  /**
   * Create a new review for a product
   */
  createReview(productId: string, request: ReviewRequest): Observable<Review> {
    return this.http.post<Review>(`${this.API_URL}/product/${productId}`, request);
  }

  /**
   * Get all reviews for a product
   */
  getProductReviews(productId: string): Observable<Review[]> {
    return this.http.get<Review[]>(`${this.API_URL}/product/${productId}`);
  }

  /**
   * Get review statistics for a product
   */
  getProductStats(productId: string): Observable<ProductReviewStats> {
    return this.http.get<ProductReviewStats>(`${this.API_URL}/product/${productId}/stats`);
  }

  /**
   * Get all reviews by the authenticated user
   */
  getMyReviews(): Observable<Review[]> {
    return this.http.get<Review[]>(`${this.API_URL}/my-reviews`);
  }

  /**
   * Get a specific review by ID
   */
  getReview(reviewId: string): Observable<Review> {
    return this.http.get<Review>(`${this.API_URL}/${reviewId}`);
  }

  /**
   * Update a review
   */
  updateReview(reviewId: string, request: ReviewRequest): Observable<Review> {
    return this.http.put<Review>(`${this.API_URL}/${reviewId}`, request);
  }

  /**
   * Delete a review
   */
  deleteReview(reviewId: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.API_URL}/${reviewId}`);
  }

  /**
   * Check if user can review a product
   */
  canReview(productId: string): Observable<{ canReview: boolean }> {
    return this.http.get<{ canReview: boolean }>(`${this.API_URL}/product/${productId}/can-review`);
  }

  /**
   * Get user's review for a product (if exists)
   */
  getMyReviewForProduct(productId: string): Observable<Review> {
    return this.http.get<Review>(`${this.API_URL}/product/${productId}/my-review`);
  }
}
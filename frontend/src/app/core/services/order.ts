import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Order,
  OrderRequest,
  OrderStatus,
  StatusUpdateRequest,
  UserOrderStats,
  SellerOrderStats,
  UserProductStats,
  SellerProductStats,
  OrderSearchParams
} from '../models/order.model';
import { resolveApiBase } from '../utils/api-host';

@Injectable({
  providedIn: 'root',
})
export class OrderService {
  private readonly apiBase = resolveApiBase(8085);
  private readonly API_URL = `${this.apiBase}/api/orders`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Create a new order (checkout)
   */
  createOrder(order: OrderRequest): Observable<Order> {
    return this.http.post<Order>(this.API_URL, order);
  }

  /**
   * Get order by ID
   */
  getOrderById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.API_URL}/${id}`);
  }

  /**
   * Get all orders for the authenticated user
   */
  getMyOrders(status?: OrderStatus): Observable<Order[]> {
    const url = status
      ? `${this.API_URL}/my-orders?status=${status}`
      : `${this.API_URL}/my-orders`;
    return this.http.get<Order[]>(url);
  }

  /**
   * Search and sort orders for the authenticated user
   */
  searchMyOrders(params: OrderSearchParams): Observable<Order[]> {
    const queryParams = this.buildSearchParams(params);
    return this.http.get<Order[]>(`${this.API_URL}/my-orders/search${queryParams}`);
  }

  /**
   * Get orders for seller (orders containing their products)
   */
  getSellerOrders(status?: OrderStatus): Observable<Order[]> {
    const url = status
      ? `${this.API_URL}/seller?status=${status}`
      : `${this.API_URL}/seller`;
    return this.http.get<Order[]>(url);
  }

  /**
   * Search and sort orders for seller
   */
  searchSellerOrders(params: OrderSearchParams): Observable<Order[]> {
    const queryParams = this.buildSearchParams(params);
    return this.http.get<Order[]>(`${this.API_URL}/seller/search${queryParams}`);
  }

  /**
   * Build query string from search params
   */
  private buildSearchParams(params: OrderSearchParams): string {
    const parts: string[] = [];
    if (params.keyword) {
      parts.push(`keyword=${encodeURIComponent(params.keyword)}`);
    }
    if (params.status) {
      parts.push(`status=${params.status}`);
    }
    if (params.sortBy) {
      parts.push(`sortBy=${params.sortBy}`);
    }
    if (params.sortDir) {
      parts.push(`sortDir=${params.sortDir}`);
    }
    return parts.length > 0 ? `?${parts.join('&')}` : '';
  }

  /**
   * Update order status (seller only)
   */
  updateOrderStatus(orderId: string, request: StatusUpdateRequest): Observable<Order> {
    return this.http.put<Order>(`${this.API_URL}/${orderId}/status`, request);
  }

  /**
   * Cancel order (customer only - when PENDING or CONFIRMED)
   */
  cancelOrder(orderId: string, reason?: string): Observable<Order> {
    return this.http.put<Order>(`${this.API_URL}/${orderId}/cancel`, { reason });
  }

  /**
   * Reorder (create new order from existing)
   */
  reorder(orderId: string): Observable<Order> {
    return this.http.post<Order>(`${this.API_URL}/${orderId}/reorder`, {});
  }

  /**
   * Get user order statistics
   */
  getUserStats(): Observable<UserOrderStats> {
    return this.http.get<UserOrderStats>(`${this.API_URL}/stats/user`);
  }

  /**
   * Get seller order statistics
   */
  getSellerStats(): Observable<SellerOrderStats> {
    return this.http.get<SellerOrderStats>(`${this.API_URL}/stats/seller`);
  }

  /**
   * Get detailed user product statistics (most purchased products)
   */
  getUserProductStats(): Observable<UserProductStats> {
    return this.http.get<UserProductStats>(`${this.API_URL}/stats/user/products`);
  }

  /**
   * Get detailed seller product statistics (best selling products)
   */
  getSellerProductStats(): Observable<SellerProductStats> {
    return this.http.get<SellerProductStats>(`${this.API_URL}/stats/seller/products`);
  }

  /**
   * Get status display text
   */
  getStatusDisplay(status: OrderStatus): string {
    const statusMap: Record<OrderStatus, string> = {
      [OrderStatus.PENDING]: 'En attente',
      [OrderStatus.CONFIRMED]: 'Confirmée',
      [OrderStatus.PROCESSING]: 'En préparation',
      [OrderStatus.SHIPPED]: 'Expédiée',
      [OrderStatus.DELIVERED]: 'Livrée',
      [OrderStatus.CANCELLED]: 'Annulée',
      [OrderStatus.REFUNDED]: 'Remboursée'
    };
    return statusMap[status] || status;
  }

  /**
   * Get status color for UI
   */
  getStatusColor(status: OrderStatus): string {
    const colorMap: Record<OrderStatus, string> = {
      [OrderStatus.PENDING]: 'warn',
      [OrderStatus.CONFIRMED]: 'primary',
      [OrderStatus.PROCESSING]: 'primary',
      [OrderStatus.SHIPPED]: 'accent',
      [OrderStatus.DELIVERED]: 'primary',
      [OrderStatus.CANCELLED]: 'warn',
      [OrderStatus.REFUNDED]: 'warn'
    };
    return colorMap[status] || 'primary';
  }
}
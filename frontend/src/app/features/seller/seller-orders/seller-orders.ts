import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

import { OrderService } from '../../../core/services/order';
import { Auth } from '../../../core/services/auth';
import { Order, OrderStatus, SellerOrderStats, OrderSearchParams } from '../../../core/models/order.model';
import { PageHeader } from '../../../shared/page-header/page-header';

@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatSnackBarModule,
    PageHeader
  ],
  templateUrl: './seller-orders.html',
  styleUrl: './seller-orders.scss'
})
export class SellerOrders implements OnInit {
  orders: Order[] = [];
  isLoading = true;
  statsLoading = true;
  selectedStatus: OrderStatus | null = null;
  stats: SellerOrderStats | null = null;

  searchKeyword = '';
  sortBy: 'createdAt' | 'totalAmount' | 'status' = 'createdAt';
  sortDir: 'asc' | 'desc' = 'desc';
  private searchSubject = new Subject<string>();

  // Track open dropdown
  openDropdownId: string | null = null;

  statusOptions = [
    { value: null, label: 'Toutes les commandes' },
    { value: OrderStatus.PENDING, label: 'En attente' },
    { value: OrderStatus.CONFIRMED, label: 'Confirmées' },
    { value: OrderStatus.PROCESSING, label: 'En préparation' },
    { value: OrderStatus.SHIPPED, label: 'Expédiées' },
    { value: OrderStatus.DELIVERED, label: 'Livrées' },
    { value: OrderStatus.CANCELLED, label: 'Annulées' }
  ];

  sortOptions = [
    { value: 'createdAt', label: 'Date' },
    { value: 'totalAmount', label: 'Montant' },
    { value: 'status', label: 'Statut' }
  ];

  statusTransitions: Record<OrderStatus, OrderStatus[]> = {
    [OrderStatus.PENDING]: [OrderStatus.CONFIRMED, OrderStatus.CANCELLED],
    [OrderStatus.CONFIRMED]: [OrderStatus.PROCESSING, OrderStatus.CANCELLED],
    [OrderStatus.PROCESSING]: [OrderStatus.SHIPPED],
    [OrderStatus.SHIPPED]: [OrderStatus.DELIVERED],
    [OrderStatus.DELIVERED]: [],
    [OrderStatus.CANCELLED]: [],
    [OrderStatus.REFUNDED]: []
  };

  constructor(
    private readonly orderService: OrderService,
    private readonly authService: Auth,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadOrders();
    this.loadStats();

    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(() => {
      this.loadOrders();
    });
  }

  loadOrders(): void {
    this.isLoading = true;
    const params: OrderSearchParams = {
      keyword: this.searchKeyword || undefined,
      status: this.selectedStatus || undefined,
      sortBy: this.sortBy,
      sortDir: this.sortDir
    };

    this.orderService.searchSellerOrders(params).subscribe({
      next: (orders) => {
        this.orders = orders;
        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        const message = error.error?.error || 'Erreur lors du chargement des commandes';
        this.snackBar.open(message, 'Erreur', { duration: 5000 });
      }
    });
  }

  onSearchChange(): void {
    this.searchSubject.next(this.searchKeyword);
  }

  onSortChange(): void {
    this.loadOrders();
  }

  toggleSortDir(): void {
    this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    this.loadOrders();
  }

  clearSearch(): void {
    this.searchKeyword = '';
    this.loadOrders();
  }

  loadStats(): void {
    this.statsLoading = true;
    this.orderService.getSellerStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.statsLoading = false;
      },
      error: () => {
        this.statsLoading = false;
      }
    });
  }

  onStatusFilterChange(): void {
    this.loadOrders();
  }

  viewOrder(order: Order): void {
    this.router.navigate(['/orders', order.id]);
  }

  toggleDropdown(orderId: string): void {
    this.openDropdownId = this.openDropdownId === orderId ? null : orderId;
  }

  updateStatus(order: Order, newStatus: OrderStatus): void {
    this.openDropdownId = null;
    const statusDisplay = this.getStatusDisplay(newStatus);

    this.orderService.updateOrderStatus(order.id, { status: newStatus }).subscribe({
      next: () => {
        this.snackBar.open(`Commande mise à jour: ${statusDisplay}`, 'OK', { duration: 3000 });
        this.loadOrders();
        this.loadStats();
      },
      error: (error) => {
        const message = error.error?.error || 'Erreur lors de la mise à jour';
        this.snackBar.open(message, 'Erreur', { duration: 5000 });
      }
    });
  }

  getAvailableTransitions(order: Order): OrderStatus[] {
    return this.statusTransitions[order.status] || [];
  }

  canUpdateStatus(order: Order): boolean {
    return this.getAvailableTransitions(order).length > 0;
  }

  getStatusDisplay(status: OrderStatus): string {
    return this.orderService.getStatusDisplay(status);
  }

  getStatusBadgeClass(status: OrderStatus): string {
    const classMap: Record<string, string> = {
      'PENDING': 'bg-warning-50 text-warning-700',
      'CONFIRMED': 'bg-primary-50 text-primary-700',
      'PROCESSING': 'bg-accent-50 text-accent-700',
      'SHIPPED': 'bg-primary-50 text-primary-700',
      'DELIVERED': 'bg-success-50 text-success-700',
      'CANCELLED': 'bg-error-50 text-error-700',
      'REFUNDED': 'bg-secondary-100 text-secondary-700'
    };
    return classMap[status] || 'bg-secondary-100 text-secondary-700';
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(amount);
  }

  goToDashboard(): void {
    this.router.navigate(['/seller/dashboard']);
  }

  goToProducts(): void {
    this.router.navigate(['/products']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  getMyItemsFromOrder(order: Order): { productName: string; quantity: number; price: number }[] {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) return [];

    return order.items
      .filter(item => item.sellerId === currentUser.id)
      .map(item => ({
        productName: item.productName,
        quantity: item.quantity,
        price: item.price
      }));
  }

  getMyItemsTotal(order: Order): number {
    return this.getMyItemsFromOrder(order)
      .reduce((sum, item) => sum + (item.price * item.quantity), 0);
  }
}
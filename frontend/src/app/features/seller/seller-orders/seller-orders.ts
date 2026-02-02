import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';

import { OrderService } from '../../../core/services/order';
import { Auth } from '../../../core/services/auth';
import { Order, OrderStatus, SellerOrderStats } from '../../../core/models/order.model';

@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatToolbarModule,
    MatMenuModule,
    MatDividerModule
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

  statusOptions = [
    { value: null, label: 'Toutes les commandes' },
    { value: OrderStatus.PENDING, label: 'En attente' },
    { value: OrderStatus.CONFIRMED, label: 'Confirmées' },
    { value: OrderStatus.PROCESSING, label: 'En préparation' },
    { value: OrderStatus.SHIPPED, label: 'Expédiées' },
    { value: OrderStatus.DELIVERED, label: 'Livrées' },
    { value: OrderStatus.CANCELLED, label: 'Annulées' }
  ];

  // Available status transitions for seller
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
  }

  loadOrders(): void {
    this.isLoading = true;
    const status = this.selectedStatus || undefined;

    this.orderService.getSellerOrders(status).subscribe({
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

  updateStatus(order: Order, newStatus: OrderStatus): void {
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

  getStatusColor(status: OrderStatus): string {
    return this.orderService.getStatusColor(status);
  }

  getStatusIcon(status: OrderStatus): string {
    const iconMap: Record<OrderStatus, string> = {
      [OrderStatus.PENDING]: 'hourglass_empty',
      [OrderStatus.CONFIRMED]: 'check_circle',
      [OrderStatus.PROCESSING]: 'inventory',
      [OrderStatus.SHIPPED]: 'local_shipping',
      [OrderStatus.DELIVERED]: 'done_all',
      [OrderStatus.CANCELLED]: 'cancel',
      [OrderStatus.REFUNDED]: 'currency_exchange'
    };
    return iconMap[status] || 'help';
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
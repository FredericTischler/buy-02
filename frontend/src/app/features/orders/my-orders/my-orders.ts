import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

import { OrderService } from '../../../core/services/order';
import { Order, OrderStatus, OrderSearchParams } from '../../../core/models/order.model';
import { PageHeader } from '../../../shared/page-header/page-header';

@Component({
  selector: 'app-my-orders',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatSnackBarModule,
    PageHeader
  ],
  templateUrl: './my-orders.html',
  styleUrl: './my-orders.scss'
})
export class MyOrders implements OnInit {
  orders: Order[] = [];
  isLoading = true;
  selectedStatus: OrderStatus | null = null;

  // Search and sort
  searchKeyword = '';
  sortBy: 'createdAt' | 'totalAmount' | 'status' = 'createdAt';
  sortDir: 'asc' | 'desc' = 'desc';
  private searchSubject = new Subject<string>();

  statusOptions = [
    { value: null, label: 'Toutes les commandes' },
    { value: OrderStatus.PENDING, label: 'En attente' },
    { value: OrderStatus.CONFIRMED, label: 'Confirmées' },
    { value: OrderStatus.SHIPPED, label: 'Expédiées' },
    { value: OrderStatus.DELIVERED, label: 'Livrées' },
    { value: OrderStatus.CANCELLED, label: 'Annulées' }
  ];

  sortOptions = [
    { value: 'createdAt', label: 'Date' },
    { value: 'totalAmount', label: 'Montant' },
    { value: 'status', label: 'Statut' }
  ];

  constructor(
    private readonly orderService: OrderService,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadOrders();

    // Set up search debounce
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(() => {
      this.loadOrders();
    });
  }

  goToProducts(): void {
    this.router.navigate(['/products']);
  }

  loadOrders(): void {
    this.isLoading = true;
    const params: OrderSearchParams = {
      keyword: this.searchKeyword || undefined,
      status: this.selectedStatus || undefined,
      sortBy: this.sortBy,
      sortDir: this.sortDir
    };

    this.orderService.searchMyOrders(params).subscribe({
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

  onStatusFilterChange(): void {
    this.loadOrders();
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

  viewOrder(order: Order): void {
    this.router.navigate(['/orders', order.id]);
  }

  cancelOrder(order: Order): void {
    if (order.status !== OrderStatus.PENDING && order.status !== OrderStatus.CONFIRMED) {
      this.snackBar.open('Cette commande ne peut pas être annulée', 'Erreur', { duration: 3000 });
      return;
    }

    if (confirm('Voulez-vous vraiment annuler cette commande ?')) {
      this.orderService.cancelOrder(order.id, 'Annulée par le client').subscribe({
        next: () => {
          this.snackBar.open('Commande annulée avec succès', 'OK', { duration: 3000 });
          this.loadOrders();
        },
        error: (error) => {
          const message = error.error?.error || 'Erreur lors de l\'annulation';
          this.snackBar.open(message, 'Erreur', { duration: 5000 });
        }
      });
    }
  }

  reorder(order: Order): void {
    this.orderService.reorder(order.id).subscribe({
      next: (newOrder) => {
        this.snackBar.open('Nouvelle commande créée!', 'OK', { duration: 3000 });
        this.router.navigate(['/orders', newOrder.id]);
      },
      error: (error) => {
        const message = error.error?.error || 'Erreur lors de la recommande';
        this.snackBar.open(message, 'Erreur', { duration: 5000 });
      }
    });
  }

  getStatusDisplay(status: OrderStatus): string {
    return this.orderService.getStatusDisplay(status);
  }

  getStatusColor(status: OrderStatus): string {
    return this.orderService.getStatusColor(status);
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

  canCancel(order: Order): boolean {
    return order.status === OrderStatus.PENDING || order.status === OrderStatus.CONFIRMED;
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
}
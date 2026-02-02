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
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { OrderService } from '../../../core/services/order';
import { Order, OrderStatus } from '../../../core/models/order.model';

@Component({
  selector: 'app-my-orders',
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
    MatTableModule,
    MatTooltipModule
  ],
  templateUrl: './my-orders.html',
  styleUrl: './my-orders.scss'
})
export class MyOrders implements OnInit {
  orders: Order[] = [];
  isLoading = true;
  selectedStatus: OrderStatus | null = null;

  displayedColumns = ['id', 'date', 'items', 'total', 'status', 'actions'];

  statusOptions = [
    { value: null, label: 'Toutes les commandes' },
    { value: OrderStatus.PENDING, label: 'En attente' },
    { value: OrderStatus.CONFIRMED, label: 'Confirmées' },
    { value: OrderStatus.SHIPPED, label: 'Expédiées' },
    { value: OrderStatus.DELIVERED, label: 'Livrées' },
    { value: OrderStatus.CANCELLED, label: 'Annulées' }
  ];

  constructor(
    private readonly orderService: OrderService,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.isLoading = true;
    const status = this.selectedStatus || undefined;

    this.orderService.getMyOrders(status).subscribe({
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

  onStatusFilterChange(): void {
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
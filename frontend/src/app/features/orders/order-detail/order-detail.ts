import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';

import { OrderService } from '../../../core/services/order';
import { Cart } from '../../../core/services/cart';
import { Order, OrderStatus } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatSnackBarModule,
    MatStepperModule,
    MatToolbarModule,
    MatBadgeModule,
    MatTooltipModule
  ],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.scss'
})
export class OrderDetail implements OnInit {
  order: Order | null = null;
  isLoading = true;
  cartCount = 0;

  statusSteps = [
    { status: OrderStatus.PENDING, label: 'En attente', icon: 'hourglass_empty' },
    { status: OrderStatus.CONFIRMED, label: 'Confirmée', icon: 'check_circle' },
    { status: OrderStatus.PROCESSING, label: 'En préparation', icon: 'inventory_2' },
    { status: OrderStatus.SHIPPED, label: 'Expédiée', icon: 'local_shipping' },
    { status: OrderStatus.DELIVERED, label: 'Livrée', icon: 'done_all' }
  ];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly location: Location,
    private readonly orderService: OrderService,
    private readonly cartService: Cart,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const orderId = this.route.snapshot.paramMap.get('id');
    if (orderId) {
      this.loadOrder(orderId);
    }
    this.updateCartCount();
    this.cartService.cartItems$.subscribe(() => {
      this.updateCartCount();
    });
  }

  updateCartCount(): void {
    this.cartCount = this.cartService.getCartCount();
  }

  goToProducts(): void {
    this.router.navigate(['/products']);
  }

  goToCart(): void {
    this.router.navigate(['/cart']);
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  loadOrder(orderId: string): void {
    this.isLoading = true;
    this.orderService.getOrderById(orderId).subscribe({
      next: (order) => {
        this.order = order;
        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        const message = error.error?.error || 'Commande introuvable';
        this.snackBar.open(message, 'Erreur', { duration: 5000 });
        this.router.navigate(['/orders']);
      }
    });
  }

  cancelOrder(): void {
    if (!this.order) return;

    if (confirm('Voulez-vous vraiment annuler cette commande ?')) {
      this.orderService.cancelOrder(this.order.id, 'Annulée par le client').subscribe({
        next: (updatedOrder) => {
          this.order = updatedOrder;
          this.snackBar.open('Commande annulée avec succès', 'OK', { duration: 3000 });
        },
        error: (error) => {
          const message = error.error?.error || 'Erreur lors de l\'annulation';
          this.snackBar.open(message, 'Erreur', { duration: 5000 });
        }
      });
    }
  }

  reorder(): void {
    if (!this.order) return;

    this.orderService.reorder(this.order.id).subscribe({
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

  canCancel(): boolean {
    return this.order?.status === OrderStatus.PENDING || this.order?.status === OrderStatus.CONFIRMED;
  }

  getCurrentStepIndex(): number {
    if (!this.order) return 0;
    if (this.order.status === OrderStatus.CANCELLED) return -1;
    return this.statusSteps.findIndex(step => step.status === this.order!.status);
  }

  isStepCompleted(stepIndex: number): boolean {
    return stepIndex < this.getCurrentStepIndex();
  }

  isStepActive(stepIndex: number): boolean {
    return stepIndex === this.getCurrentStepIndex();
  }

  formatDate(date: Date | undefined): string {
    if (!date) return '-';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  goBack(): void {
    this.location.back();
  }
}
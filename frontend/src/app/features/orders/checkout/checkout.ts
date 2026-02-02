import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { Cart } from '../../../core/services/cart';
import { OrderService } from '../../../core/services/order';
import { CartItem } from '../../../core/models/cart.model';
import { OrderRequest, OrderItemRequest } from '../../../core/models/order.model';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './checkout.html',
  styleUrl: './checkout.scss'
})
export class Checkout implements OnInit {
  checkoutForm!: FormGroup;
  cartItems: CartItem[] = [];
  cartTotal = 0;
  isLoading = false;
  isSubmitting = false;

  paymentMethods = [
    { value: 'COD', label: 'Paiement à la livraison (Cash on Delivery)' }
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly cartService: Cart,
    private readonly orderService: OrderService,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadCart();
  }

  initForm(): void {
    this.checkoutForm = this.fb.group({
      shippingAddress: ['', [Validators.required, Validators.minLength(5)]],
      shippingCity: ['', [Validators.required]],
      shippingPostalCode: ['', [Validators.required, Validators.pattern(/^\d{5}$/)]],
      shippingCountry: ['France', [Validators.required]],
      phoneNumber: ['', [Validators.required, Validators.pattern(/^(\+33|0)[1-9](\d{2}){4}$/)]],
      paymentMethod: ['COD', [Validators.required]],
      notes: ['']
    });
  }

  loadCart(): void {
    this.isLoading = true;
    this.cartService.cartItems$.subscribe(items => {
      this.cartItems = items;
      this.cartTotal = this.cartService.getCartTotal();
      this.isLoading = false;

      if (items.length === 0) {
        this.snackBar.open('Votre panier est vide', 'OK', { duration: 3000 });
        this.router.navigate(['/cart']);
      }
    });
  }

  onSubmit(): void {
    if (this.checkoutForm.invalid) {
      this.markFormGroupTouched();
      return;
    }

    if (this.cartItems.length === 0) {
      this.snackBar.open('Votre panier est vide', 'Erreur', { duration: 3000 });
      return;
    }

    this.isSubmitting = true;

    const orderItems: OrderItemRequest[] = this.cartItems.map(item => ({
      productId: item.productId,
      productName: item.name,
      sellerId: '', // Will be filled from product data
      sellerName: '',
      price: item.price,
      quantity: item.quantity,
      imageUrl: item.imageUrl || undefined
    }));

    const orderRequest: OrderRequest = {
      items: orderItems,
      shippingAddress: this.checkoutForm.value.shippingAddress,
      shippingCity: this.checkoutForm.value.shippingCity,
      shippingPostalCode: this.checkoutForm.value.shippingPostalCode,
      shippingCountry: this.checkoutForm.value.shippingCountry,
      phoneNumber: this.checkoutForm.value.phoneNumber,
      paymentMethod: this.checkoutForm.value.paymentMethod,
      notes: this.checkoutForm.value.notes || undefined
    };

    this.orderService.createOrder(orderRequest).subscribe({
      next: (order) => {
        this.isSubmitting = false;
        this.cartService.clearCart();
        this.snackBar.open('Commande créée avec succès!', 'OK', { duration: 3000 });
        this.router.navigate(['/orders', order.id]);
      },
      error: (error) => {
        this.isSubmitting = false;
        const message = error.error?.error || 'Erreur lors de la création de la commande';
        this.snackBar.open(message, 'Erreur', { duration: 5000 });
      }
    });
  }

  private markFormGroupTouched(): void {
    Object.keys(this.checkoutForm.controls).forEach(key => {
      this.checkoutForm.get(key)?.markAsTouched();
    });
  }

  goBack(): void {
    this.router.navigate(['/cart']);
  }
}
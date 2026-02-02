import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Checkout } from './checkout';
import { Cart } from '../../../core/services/cart';
import { OrderService } from '../../../core/services/order';
import { CartItem } from '../../../core/models/cart.model';
import { Order, OrderStatus } from '../../../core/models/order.model';

describe('Checkout', () => {
  let component: Checkout;
  let fixture: ComponentFixture<Checkout>;
  let cartService: jasmine.SpyObj<Cart>;
  let orderService: jasmine.SpyObj<OrderService>;
  let router: Router;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let cartStream: BehaviorSubject<CartItem[]>;

  const mockCartItems: CartItem[] = [
    {
      productId: 'p1',
      name: 'Product 1',
      price: 100,
      quantity: 2,
      imageUrl: '/img1.png',
      sellerId: 'seller-1',
      sellerName: 'Test Seller',
      stock: 10
    },
    {
      productId: 'p2',
      name: 'Product 2',
      price: 50,
      quantity: 1,
      imageUrl: '/img2.png',
      sellerId: 'seller-2',
      sellerName: 'Another Seller',
      stock: 5
    }
  ];

  const mockOrder: Order = {
    id: 'order-1',
    userId: 'user-1',
    userName: 'Test User',
    userEmail: 'test@test.com',
    items: [],
    totalAmount: 250,
    status: OrderStatus.PENDING,
    shippingAddress: '123 Test St',
    shippingCity: 'Paris',
    shippingPostalCode: '75001',
    shippingCountry: 'France',
    phoneNumber: '0612345678',
    paymentMethod: 'COD',
    createdAt: new Date()
  };

  beforeEach(async () => {
    cartStream = new BehaviorSubject<CartItem[]>(mockCartItems);

    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    cartService = jasmine.createSpyObj<Cart>('Cart', [
      'getCartTotal',
      'clearCart'
    ]);
    cartService.cartItems$ = cartStream.asObservable();
    cartService.getCartTotal.and.returnValue(250);

    orderService = jasmine.createSpyObj<OrderService>('OrderService', ['createOrder']);
    orderService.createOrder.and.returnValue(of(mockOrder));

    await TestBed.configureTestingModule({
      imports: [
        Checkout,
        RouterTestingModule.withRoutes([]),
        HttpClientTestingModule,
        NoopAnimationsModule,
        ReactiveFormsModule
      ],
      providers: [
        { provide: Cart, useValue: cartService },
        { provide: OrderService, useValue: orderService },
        { provide: MatSnackBar, useValue: snackBar }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Checkout);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with default values', () => {
    expect(component.checkoutForm).toBeDefined();
    expect(component.checkoutForm.get('shippingCountry')?.value).toBe('France');
    expect(component.checkoutForm.get('paymentMethod')?.value).toBe('COD');
  });

  it('should load cart items on init', () => {
    expect(component.cartItems).toEqual(mockCartItems);
    expect(component.cartTotal).toBe(250);
  });

  it('should redirect to cart when cart is empty', fakeAsync(() => {
    cartStream.next([]);
    tick();

    expect(router.navigate).toHaveBeenCalledWith(['/cart']);
  }));

  it('should have required validators on form fields', () => {
    const form = component.checkoutForm;

    form.get('shippingAddress')?.setValue('');
    form.get('shippingCity')?.setValue('');
    form.get('shippingPostalCode')?.setValue('');
    form.get('phoneNumber')?.setValue('');

    expect(form.valid).toBeFalse();
    expect(form.get('shippingAddress')?.hasError('required')).toBeTrue();
    expect(form.get('shippingCity')?.hasError('required')).toBeTrue();
    expect(form.get('shippingPostalCode')?.hasError('required')).toBeTrue();
    expect(form.get('phoneNumber')?.hasError('required')).toBeTrue();
  });

  it('should validate postal code format', () => {
    const postalCodeControl = component.checkoutForm.get('shippingPostalCode');

    postalCodeControl?.setValue('12345');
    expect(postalCodeControl?.valid).toBeTrue();

    postalCodeControl?.setValue('1234');
    expect(postalCodeControl?.hasError('pattern')).toBeTrue();

    postalCodeControl?.setValue('abcde');
    expect(postalCodeControl?.hasError('pattern')).toBeTrue();
  });

  it('should validate French phone number format', () => {
    const phoneControl = component.checkoutForm.get('phoneNumber');

    phoneControl?.setValue('0612345678');
    expect(phoneControl?.valid).toBeTrue();

    phoneControl?.setValue('+33612345678');
    expect(phoneControl?.valid).toBeTrue();

    phoneControl?.setValue('123456');
    expect(phoneControl?.hasError('pattern')).toBeTrue();
  });

  it('should not submit when form is invalid', () => {
    component.checkoutForm.get('shippingAddress')?.setValue('');

    component.onSubmit();

    expect(orderService.createOrder).not.toHaveBeenCalled();
  });

  it('should not submit when cart is empty', () => {
    component.cartItems = [];
    component.checkoutForm.patchValue({
      shippingAddress: '123 Test St',
      shippingCity: 'Paris',
      shippingPostalCode: '75001',
      phoneNumber: '0612345678'
    });

    component.onSubmit();

    expect(orderService.createOrder).not.toHaveBeenCalled();
  });

  it('should create order successfully', fakeAsync(() => {
    component.checkoutForm.patchValue({
      shippingAddress: '123 Test St',
      shippingCity: 'Paris',
      shippingPostalCode: '75001',
      phoneNumber: '0612345678'
    });

    component.onSubmit();
    tick();

    expect(orderService.createOrder).toHaveBeenCalled();
    expect(cartService.clearCart).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/orders', 'order-1']);
  }));

  it('should handle order creation error', fakeAsync(() => {
    orderService.createOrder.and.returnValue(throwError(() => ({ error: { error: 'Stock insuffisant' } })));

    component.checkoutForm.patchValue({
      shippingAddress: '123 Test St',
      shippingCity: 'Paris',
      shippingPostalCode: '75001',
      phoneNumber: '0612345678'
    });

    component.onSubmit();
    tick();

    expect(component.isSubmitting).toBeFalse();
  }));

  it('should navigate back to cart', () => {
    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/cart']);
  });

  it('should navigate to products', () => {
    component.goToProducts();

    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });

  it('should navigate to profile', () => {
    component.goToProfile();

    expect(router.navigate).toHaveBeenCalledWith(['/profile']);
  });

  it('should have COD as payment method option', () => {
    expect(component.paymentMethods).toContain(
      jasmine.objectContaining({ value: 'COD' })
    );
  });
});
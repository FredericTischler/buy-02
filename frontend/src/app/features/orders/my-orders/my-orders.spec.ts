import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { MyOrders } from './my-orders';
import { OrderService } from '../../../core/services/order';
import { Auth } from '../../../core/services/auth';
import { Cart } from '../../../core/services/cart';
import { Order, OrderStatus } from '../../../core/models/order.model';
import { CartItem } from '../../../core/models/cart.model';

describe('MyOrders', () => {
  let component: MyOrders;
  let fixture: ComponentFixture<MyOrders>;
  let orderService: jasmine.SpyObj<OrderService>;
  let authService: jasmine.SpyObj<Auth>;
  let cartService: jasmine.SpyObj<Cart>;
  let router: Router;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let cartStream: BehaviorSubject<CartItem[]>;

  const mockOrders: Order[] = [
    {
      id: 'order-1',
      userId: 'user-1',
      userName: 'Test User',
      userEmail: 'test@test.com',
      items: [{ productId: 'p1', productName: 'Product 1', sellerId: 's1', sellerName: 'Seller', price: 100, quantity: 2 }],
      totalAmount: 200,
      status: OrderStatus.PENDING,
      shippingAddress: '123 Test St',
      shippingCity: 'Paris',
      shippingPostalCode: '75001',
      shippingCountry: 'France',
      phoneNumber: '0612345678',
      paymentMethod: 'COD',
      createdAt: new Date()
    },
    {
      id: 'order-2',
      userId: 'user-1',
      userName: 'Test User',
      userEmail: 'test@test.com',
      items: [{ productId: 'p2', productName: 'Product 2', sellerId: 's2', sellerName: 'Seller 2', price: 50, quantity: 1 }],
      totalAmount: 50,
      status: OrderStatus.DELIVERED,
      shippingAddress: '456 Test Ave',
      shippingCity: 'Lyon',
      shippingPostalCode: '69001',
      shippingCountry: 'France',
      phoneNumber: '0698765432',
      paymentMethod: 'COD',
      createdAt: new Date()
    }
  ];

  beforeEach(async () => {
    cartStream = new BehaviorSubject<CartItem[]>([]);

    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    orderService = jasmine.createSpyObj<OrderService>('OrderService', [
      'searchMyOrders',
      'cancelOrder',
      'reorder',
      'getStatusDisplay',
      'getStatusColor'
    ]);
    orderService.searchMyOrders.and.returnValue(of(mockOrders));
    orderService.getStatusDisplay.and.callFake((status: OrderStatus) => {
      const map: Record<OrderStatus, string> = {
        [OrderStatus.PENDING]: 'En attente',
        [OrderStatus.CONFIRMED]: 'Confirmée',
        [OrderStatus.PROCESSING]: 'En préparation',
        [OrderStatus.SHIPPED]: 'Expédiée',
        [OrderStatus.DELIVERED]: 'Livrée',
        [OrderStatus.CANCELLED]: 'Annulée',
        [OrderStatus.REFUNDED]: 'Remboursée'
      };
      return map[status];
    });
    orderService.getStatusColor.and.returnValue('primary');

    authService = jasmine.createSpyObj<Auth>('Auth', ['logout']);

    cartService = jasmine.createSpyObj<Cart>('Cart', ['getCartCount']);
    cartService.cartItems$ = cartStream.asObservable();
    cartService.getCartCount.and.returnValue(0);

    await TestBed.configureTestingModule({
      imports: [
        MyOrders,
        RouterTestingModule.withRoutes([]),
        HttpClientTestingModule,
        NoopAnimationsModule,
        FormsModule
      ],
      providers: [
        { provide: OrderService, useValue: orderService },
        { provide: Auth, useValue: authService },
        { provide: Cart, useValue: cartService },
        { provide: MatSnackBar, useValue: snackBar }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MyOrders);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load orders on init', () => {
    expect(orderService.searchMyOrders).toHaveBeenCalled();
    expect(component.orders).toEqual(mockOrders);
    expect(component.isLoading).toBeFalse();
  });

  it('should handle order loading error', fakeAsync(() => {
    orderService.searchMyOrders.and.returnValue(throwError(() => ({ error: { error: 'Erreur serveur' } })));

    component.loadOrders();
    tick();

    expect(component.isLoading).toBeFalse();
  }));

  it('should filter orders by status', () => {
    component.selectedStatus = OrderStatus.PENDING;
    component.onStatusFilterChange();

    expect(orderService.searchMyOrders).toHaveBeenCalledWith(
      jasmine.objectContaining({ status: OrderStatus.PENDING })
    );
  });

  it('should search orders with keyword', fakeAsync(() => {
    component.searchKeyword = 'test';
    component.onSearchChange();
    tick(300);

    expect(orderService.searchMyOrders).toHaveBeenCalledWith(
      jasmine.objectContaining({ keyword: 'test' })
    );
  }));

  it('should sort orders', () => {
    component.sortBy = 'totalAmount';
    component.onSortChange();

    expect(orderService.searchMyOrders).toHaveBeenCalledWith(
      jasmine.objectContaining({ sortBy: 'totalAmount' })
    );
  });

  it('should toggle sort direction', () => {
    expect(component.sortDir).toBe('desc');

    component.toggleSortDir();

    expect(component.sortDir).toBe('asc');
    expect(orderService.searchMyOrders).toHaveBeenCalled();
  });

  it('should clear search', () => {
    component.searchKeyword = 'test';
    component.clearSearch();

    expect(component.searchKeyword).toBe('');
    expect(orderService.searchMyOrders).toHaveBeenCalled();
  });

  it('should navigate to order detail', () => {
    component.viewOrder(mockOrders[0]);

    expect(router.navigate).toHaveBeenCalledWith(['/orders', 'order-1']);
  });

  it('should cancel order when confirmed', fakeAsync(() => {
    spyOn(window, 'confirm').and.returnValue(true);
    orderService.cancelOrder.and.returnValue(of(mockOrders[0]));

    component.cancelOrder(mockOrders[0]);
    tick();

    expect(orderService.cancelOrder).toHaveBeenCalledWith('order-1', 'Annulée par le client');
  }));

  it('should not cancel order when declined', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.cancelOrder(mockOrders[0]);

    expect(orderService.cancelOrder).not.toHaveBeenCalled();
  });

  it('should not cancel order with invalid status', () => {
    const deliveredOrder = { ...mockOrders[1], status: OrderStatus.DELIVERED };

    component.cancelOrder(deliveredOrder);

    expect(orderService.cancelOrder).not.toHaveBeenCalled();
  });

  it('should reorder successfully', fakeAsync(() => {
    const newOrder = { ...mockOrders[0], id: 'order-3' };
    orderService.reorder.and.returnValue(of(newOrder));

    component.reorder(mockOrders[0]);
    tick();

    expect(orderService.reorder).toHaveBeenCalledWith('order-1');
    expect(router.navigate).toHaveBeenCalledWith(['/orders', 'order-3']);
  }));

  it('should handle reorder error', fakeAsync(() => {
    orderService.reorder.and.returnValue(throwError(() => ({ error: { error: 'Stock insuffisant' } })));

    component.reorder(mockOrders[0]);
    tick();

    expect(orderService.reorder).toHaveBeenCalledWith('order-1');
  }));

  it('should check if order can be cancelled', () => {
    expect(component.canCancel({ ...mockOrders[0], status: OrderStatus.PENDING })).toBeTrue();
    expect(component.canCancel({ ...mockOrders[0], status: OrderStatus.CONFIRMED })).toBeTrue();
    expect(component.canCancel({ ...mockOrders[0], status: OrderStatus.SHIPPED })).toBeFalse();
    expect(component.canCancel({ ...mockOrders[0], status: OrderStatus.DELIVERED })).toBeFalse();
  });

  it('should format date correctly', () => {
    const date = new Date('2024-01-15T10:30:00');
    const formatted = component.formatDate(date);

    expect(formatted).toContain('15');
    expect(formatted).toContain('01');
    expect(formatted).toContain('2024');
  });

  it('should navigate to products', () => {
    component.goToProducts();
    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });

  it('should navigate to cart', () => {
    component.goToCart();
    expect(router.navigate).toHaveBeenCalledWith(['/cart']);
  });

  it('should navigate to profile', () => {
    component.goToProfile();
    expect(router.navigate).toHaveBeenCalledWith(['/profile']);
  });

  it('should logout and navigate to login', () => {
    component.logout();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should update cart count', () => {
    cartService.getCartCount.and.returnValue(5);

    component.updateCartCount();

    expect(component.cartCount).toBe(5);
  });
});
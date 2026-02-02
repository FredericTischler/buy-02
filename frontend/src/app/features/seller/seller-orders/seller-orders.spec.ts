import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { SellerOrders } from './seller-orders';
import { OrderService } from '../../../core/services/order';
import { Auth } from '../../../core/services/auth';
import { Order, OrderStatus, SellerOrderStats } from '../../../core/models/order.model';
import { User } from '../../../core/models/user.model';

describe('SellerOrders', () => {
  let component: SellerOrders;
  let fixture: ComponentFixture<SellerOrders>;
  let orderService: jasmine.SpyObj<OrderService>;
  let authService: jasmine.SpyObj<Auth>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let router: Router;

  const mockUser: User = {
    id: 'seller-1',
    name: 'Test Seller',
    email: 'seller@example.com',
    role: 'SELLER',
    createdAt: new Date()
  };

  const mockStats: SellerOrderStats = {
    totalOrders: 15,
    completedOrders: 10,
    totalRevenue: 2500.00,
    totalItemsSold: 25
  };

  const mockOrders: Order[] = [
    {
      id: 'order-1',
      userId: 'user-1',
      userName: 'John Doe',
      userEmail: 'john@example.com',
      items: [
        {
          productId: 'prod-1',
          productName: 'Test Product',
          sellerId: 'seller-1',
          sellerName: 'Test Seller',
          price: 100,
          quantity: 2
        }
      ],
      totalAmount: 200,
      status: OrderStatus.PENDING,
      shippingAddress: '123 Test St',
      shippingCity: 'Paris',
      shippingPostalCode: '75001',
      shippingCountry: 'France',
      phoneNumber: '0612345678',
      paymentMethod: 'card',
      createdAt: new Date()
    },
    {
      id: 'order-2',
      userId: 'user-2',
      userName: 'Jane Doe',
      userEmail: 'jane@example.com',
      items: [
        {
          productId: 'prod-2',
          productName: 'Another Product',
          sellerId: 'seller-1',
          sellerName: 'Test Seller',
          price: 50,
          quantity: 1
        }
      ],
      totalAmount: 50,
      status: OrderStatus.CONFIRMED,
      shippingAddress: '456 Test Ave',
      shippingCity: 'Lyon',
      shippingPostalCode: '69001',
      shippingCountry: 'France',
      phoneNumber: '0698765432',
      paymentMethod: 'card',
      createdAt: new Date()
    }
  ];

  beforeEach(async () => {
    orderService = jasmine.createSpyObj<OrderService>('OrderService', [
      'getSellerOrders',
      'getSellerStats',
      'updateOrderStatus',
      'getStatusDisplay',
      'getStatusColor'
    ]);
    authService = jasmine.createSpyObj<Auth>('Auth', ['getCurrentUser', 'logout']);
    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    orderService.getSellerOrders.and.returnValue(of(mockOrders));
    orderService.getSellerStats.and.returnValue(of(mockStats));
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
    authService.getCurrentUser.and.returnValue(mockUser);

    await TestBed.configureTestingModule({
      imports: [
        SellerOrders,
        RouterTestingModule.withRoutes([]),
        NoopAnimationsModule
      ],
      providers: [
        { provide: OrderService, useValue: orderService },
        { provide: Auth, useValue: authService },
        { provide: MatSnackBar, useValue: snackBar }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SellerOrders);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    (component as any).snackBar = snackBar;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load orders on init', () => {
    expect(orderService.getSellerOrders).toHaveBeenCalled();
    expect(component.orders).toEqual(mockOrders);
    expect(component.isLoading).toBeFalse();
  });

  it('should load stats on init', () => {
    expect(orderService.getSellerStats).toHaveBeenCalled();
    expect(component.stats).toEqual(mockStats);
    expect(component.statsLoading).toBeFalse();
  });

  it('should handle orders loading error', () => {
    orderService.getSellerOrders.and.returnValue(throwError(() => ({ error: { error: 'Test error' } })));

    component.loadOrders();

    expect(component.isLoading).toBeFalse();
    expect(snackBar.open).toHaveBeenCalledWith('Test error', 'Erreur', jasmine.any(Object));
  });

  it('should filter orders by status', () => {
    component.selectedStatus = OrderStatus.PENDING;
    component.onStatusFilterChange();

    expect(orderService.getSellerOrders).toHaveBeenCalledWith(OrderStatus.PENDING);
  });

  it('should navigate to order details', () => {
    component.viewOrder(mockOrders[0]);

    expect(router.navigate).toHaveBeenCalledWith(['/orders', 'order-1']);
  });

  it('should update order status', () => {
    orderService.updateOrderStatus.and.returnValue(of(mockOrders[0]));

    component.updateStatus(mockOrders[0], OrderStatus.CONFIRMED);

    expect(orderService.updateOrderStatus).toHaveBeenCalledWith('order-1', { status: OrderStatus.CONFIRMED });
    expect(snackBar.open).toHaveBeenCalledWith('Commande mise à jour: Confirmée', 'OK', jasmine.any(Object));
  });

  it('should handle status update error', () => {
    orderService.updateOrderStatus.and.returnValue(throwError(() => ({ error: { error: 'Update failed' } })));

    component.updateStatus(mockOrders[0], OrderStatus.CONFIRMED);

    expect(snackBar.open).toHaveBeenCalledWith('Update failed', 'Erreur', jasmine.any(Object));
  });

  it('should return correct available transitions for PENDING', () => {
    const pendingOrder = { ...mockOrders[0], status: OrderStatus.PENDING };
    const transitions = component.getAvailableTransitions(pendingOrder);

    expect(transitions).toContain(OrderStatus.CONFIRMED);
    expect(transitions).toContain(OrderStatus.CANCELLED);
  });

  it('should return correct available transitions for CONFIRMED', () => {
    const confirmedOrder = { ...mockOrders[0], status: OrderStatus.CONFIRMED };
    const transitions = component.getAvailableTransitions(confirmedOrder);

    expect(transitions).toContain(OrderStatus.PROCESSING);
    expect(transitions).toContain(OrderStatus.CANCELLED);
  });

  it('should return no transitions for DELIVERED', () => {
    const deliveredOrder = { ...mockOrders[0], status: OrderStatus.DELIVERED };
    const transitions = component.getAvailableTransitions(deliveredOrder);

    expect(transitions.length).toBe(0);
  });

  it('should correctly determine if status can be updated', () => {
    const pendingOrder = { ...mockOrders[0], status: OrderStatus.PENDING };
    const deliveredOrder = { ...mockOrders[0], status: OrderStatus.DELIVERED };

    expect(component.canUpdateStatus(pendingOrder)).toBeTrue();
    expect(component.canUpdateStatus(deliveredOrder)).toBeFalse();
  });

  it('should get correct status icon', () => {
    expect(component.getStatusIcon(OrderStatus.PENDING)).toBe('hourglass_empty');
    expect(component.getStatusIcon(OrderStatus.SHIPPED)).toBe('local_shipping');
    expect(component.getStatusIcon(OrderStatus.DELIVERED)).toBe('done_all');
  });

  it('should format date correctly', () => {
    const date = new Date('2024-01-15T10:30:00');
    const formatted = component.formatDate(date);

    expect(formatted).toContain('2024');
  });

  it('should format currency correctly', () => {
    const formatted = component.formatCurrency(1500.50);

    expect(formatted).toContain('1');
    expect(formatted).toContain('500');
  });

  it('should navigate to dashboard', () => {
    component.goToDashboard();

    expect(router.navigate).toHaveBeenCalledWith(['/seller/dashboard']);
  });

  it('should navigate to products', () => {
    component.goToProducts();

    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });

  it('should logout and navigate to login', () => {
    component.logout();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should get my items from order', () => {
    const items = component.getMyItemsFromOrder(mockOrders[0]);

    expect(items.length).toBe(1);
    expect(items[0].productName).toBe('Test Product');
    expect(items[0].quantity).toBe(2);
  });

  it('should calculate my items total', () => {
    const total = component.getMyItemsTotal(mockOrders[0]);

    expect(total).toBe(200); // 100 * 2
  });

  it('should return empty array when no user for getMyItemsFromOrder', () => {
    authService.getCurrentUser.and.returnValue(null);

    const items = component.getMyItemsFromOrder(mockOrders[0]);

    expect(items).toEqual([]);
  });
});
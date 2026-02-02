import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { OrderDetail } from './order-detail';
import { OrderService } from '../../../core/services/order';
import { Cart } from '../../../core/services/cart';
import { Order, OrderStatus } from '../../../core/models/order.model';
import { CartItem } from '../../../core/models/cart.model';

describe('OrderDetail', () => {
  let component: OrderDetail;
  let fixture: ComponentFixture<OrderDetail>;
  let orderService: jasmine.SpyObj<OrderService>;
  let cartService: jasmine.SpyObj<Cart>;
  let router: Router;
  let location: jasmine.SpyObj<Location>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let cartStream: BehaviorSubject<CartItem[]>;

  const mockOrder: Order = {
    id: 'order-1',
    userId: 'user-1',
    userName: 'Test User',
    userEmail: 'test@test.com',
    items: [
      { productId: 'p1', productName: 'Product 1', sellerId: 's1', sellerName: 'Seller', price: 100, quantity: 2, imageUrl: '/img1.png' }
    ],
    totalAmount: 200,
    status: OrderStatus.PENDING,
    shippingAddress: '123 Test St',
    shippingCity: 'Paris',
    shippingPostalCode: '75001',
    shippingCountry: 'France',
    phoneNumber: '0612345678',
    paymentMethod: 'COD',
    createdAt: new Date(),
    confirmedAt: undefined,
    shippedAt: undefined,
    deliveredAt: undefined
  };

  beforeEach(async () => {
    cartStream = new BehaviorSubject<CartItem[]>([]);

    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    location = jasmine.createSpyObj<Location>('Location', ['back']);

    orderService = jasmine.createSpyObj<OrderService>('OrderService', [
      'getOrderById',
      'cancelOrder',
      'reorder',
      'getStatusDisplay',
      'getStatusColor'
    ]);
    orderService.getOrderById.and.returnValue(of(mockOrder));
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

    cartService = jasmine.createSpyObj<Cart>('Cart', ['getCartCount']);
    cartService.cartItems$ = cartStream.asObservable();
    cartService.getCartCount.and.returnValue(0);

    await TestBed.configureTestingModule({
      imports: [
        OrderDetail,
        RouterTestingModule.withRoutes([]),
        NoopAnimationsModule
      ],
      providers: [
        { provide: OrderService, useValue: orderService },
        { provide: Cart, useValue: cartService },
        { provide: Location, useValue: location },
        { provide: MatSnackBar, useValue: snackBar },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => 'order-1'
              }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrderDetail);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load order on init', () => {
    expect(orderService.getOrderById).toHaveBeenCalledWith('order-1');
    expect(component.order).toEqual(mockOrder);
    expect(component.isLoading).toBeFalse();
  });

  it('should handle order loading error', fakeAsync(() => {
    orderService.getOrderById.and.returnValue(throwError(() => ({ error: { error: 'Commande introuvable' } })));

    component.loadOrder('invalid-id');
    tick();

    expect(component.isLoading).toBeFalse();
    expect(snackBar.open).toHaveBeenCalledWith('Commande introuvable', 'Erreur', { duration: 5000 });
    expect(router.navigate).toHaveBeenCalledWith(['/orders']);
  }));

  it('should cancel order when confirmed', fakeAsync(() => {
    spyOn(window, 'confirm').and.returnValue(true);
    const cancelledOrder = { ...mockOrder, status: OrderStatus.CANCELLED };
    orderService.cancelOrder.and.returnValue(of(cancelledOrder));

    component.cancelOrder();
    tick();

    expect(orderService.cancelOrder).toHaveBeenCalledWith('order-1', 'Annulée par le client');
    expect(component.order?.status).toBe(OrderStatus.CANCELLED);
    expect(snackBar.open).toHaveBeenCalledWith('Commande annulée avec succès', 'OK', { duration: 3000 });
  }));

  it('should not cancel order when declined', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.cancelOrder();

    expect(orderService.cancelOrder).not.toHaveBeenCalled();
  });

  it('should not cancel when order is null', () => {
    component.order = null;

    component.cancelOrder();

    expect(orderService.cancelOrder).not.toHaveBeenCalled();
  });

  it('should handle cancel order error', fakeAsync(() => {
    spyOn(window, 'confirm').and.returnValue(true);
    orderService.cancelOrder.and.returnValue(throwError(() => ({ error: { error: 'Impossible d\'annuler' } })));

    component.cancelOrder();
    tick();

    expect(snackBar.open).toHaveBeenCalledWith('Impossible d\'annuler', 'Erreur', { duration: 5000 });
  }));

  it('should reorder successfully', fakeAsync(() => {
    const newOrder = { ...mockOrder, id: 'order-2' };
    orderService.reorder.and.returnValue(of(newOrder));

    component.reorder();
    tick();

    expect(orderService.reorder).toHaveBeenCalledWith('order-1');
    expect(snackBar.open).toHaveBeenCalledWith('Nouvelle commande créée!', 'OK', { duration: 3000 });
    expect(router.navigate).toHaveBeenCalledWith(['/orders', 'order-2']);
  }));

  it('should not reorder when order is null', () => {
    component.order = null;

    component.reorder();

    expect(orderService.reorder).not.toHaveBeenCalled();
  });

  it('should handle reorder error', fakeAsync(() => {
    orderService.reorder.and.returnValue(throwError(() => ({ error: { error: 'Stock insuffisant' } })));

    component.reorder();
    tick();

    expect(snackBar.open).toHaveBeenCalledWith('Stock insuffisant', 'Erreur', { duration: 5000 });
  }));

  it('should check if order can be cancelled', () => {
    component.order = { ...mockOrder, status: OrderStatus.PENDING };
    expect(component.canCancel()).toBeTrue();

    component.order = { ...mockOrder, status: OrderStatus.CONFIRMED };
    expect(component.canCancel()).toBeTrue();

    component.order = { ...mockOrder, status: OrderStatus.SHIPPED };
    expect(component.canCancel()).toBeFalse();

    component.order = null;
    expect(component.canCancel()).toBeFalse();
  });

  it('should get current step index for status', () => {
    component.order = { ...mockOrder, status: OrderStatus.PENDING };
    expect(component.getCurrentStepIndex()).toBe(0);

    component.order = { ...mockOrder, status: OrderStatus.CONFIRMED };
    expect(component.getCurrentStepIndex()).toBe(1);

    component.order = { ...mockOrder, status: OrderStatus.SHIPPED };
    expect(component.getCurrentStepIndex()).toBe(3);

    component.order = { ...mockOrder, status: OrderStatus.CANCELLED };
    expect(component.getCurrentStepIndex()).toBe(-1);
  });

  it('should check if step is completed', () => {
    component.order = { ...mockOrder, status: OrderStatus.SHIPPED };

    expect(component.isStepCompleted(0)).toBeTrue();
    expect(component.isStepCompleted(1)).toBeTrue();
    expect(component.isStepCompleted(2)).toBeTrue();
    expect(component.isStepCompleted(3)).toBeFalse();
  });

  it('should check if step is active', () => {
    component.order = { ...mockOrder, status: OrderStatus.CONFIRMED };

    expect(component.isStepActive(0)).toBeFalse();
    expect(component.isStepActive(1)).toBeTrue();
    expect(component.isStepActive(2)).toBeFalse();
  });

  it('should format date correctly', () => {
    const date = new Date('2024-01-15T10:30:00');
    const formatted = component.formatDate(date);

    expect(formatted).toContain('15');
    expect(formatted).toContain('01');
    expect(formatted).toContain('2024');
  });

  it('should return dash for undefined date', () => {
    expect(component.formatDate(undefined)).toBe('-');
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

  it('should go back using location', () => {
    component.goBack();
    expect(location.back).toHaveBeenCalled();
  });

  it('should update cart count', () => {
    cartService.getCartCount.and.returnValue(3);

    component.updateCartCount();

    expect(component.cartCount).toBe(3);
  });

  it('should have correct status steps defined', () => {
    expect(component.statusSteps.length).toBe(5);
    expect(component.statusSteps[0].status).toBe(OrderStatus.PENDING);
    expect(component.statusSteps[4].status).toBe(OrderStatus.DELIVERED);
  });
});
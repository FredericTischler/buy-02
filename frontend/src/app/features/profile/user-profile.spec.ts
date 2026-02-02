import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { UserProfile } from './user-profile';
import { Auth } from '../../core/services/auth';
import { OrderService } from '../../core/services/order';
import { Cart } from '../../core/services/cart';
import { User } from '../../core/models/user.model';
import { UserOrderStats } from '../../core/models/order.model';
import { CartItem } from '../../core/models/cart.model';

describe('UserProfile', () => {
  let component: UserProfile;
  let fixture: ComponentFixture<UserProfile>;
  let authService: jasmine.SpyObj<Auth>;
  let orderService: jasmine.SpyObj<OrderService>;
  let cartService: Cart;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let router: Router;
  let cartStream: BehaviorSubject<CartItem[]>;

  const mockUser: User = {
    id: 'user-1',
    name: 'John Doe',
    email: 'john@example.com',
    role: 'CLIENT',
    avatar: '/uploads/avatar.jpg',
    createdAt: new Date('2024-01-15'),
  };

  const mockStats: UserOrderStats = {
    totalOrders: 10,
    completedOrders: 8,
    totalSpent: 1500.50,
  };

  beforeEach(async () => {
    authService = jasmine.createSpyObj<Auth>('Auth', ['getCurrentUser', 'logout']);
    orderService = jasmine.createSpyObj<OrderService>('OrderService', ['getUserStats']);
    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    cartStream = new BehaviorSubject<CartItem[]>([]);

    cartService = {
      cartItems$: cartStream.asObservable(),
      getCartCount: jasmine.createSpy('getCartCount').and.returnValue(0),
    } as unknown as Cart;

    authService.getCurrentUser.and.returnValue(mockUser);
    orderService.getUserStats.and.returnValue(of(mockStats));

    await TestBed.configureTestingModule({
      imports: [
        UserProfile,
        RouterTestingModule.withRoutes([]),
        NoopAnimationsModule,
      ],
      providers: [
        { provide: Auth, useValue: authService },
        { provide: OrderService, useValue: orderService },
        { provide: Cart, useValue: cartService },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserProfile);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    (component as any).snackBar = snackBar;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load user profile on init', () => {
    expect(authService.getCurrentUser).toHaveBeenCalled();
    expect(component.user).toEqual(mockUser);
    expect(component.isLoading).toBeFalse();
  });

  it('should load user stats on init', () => {
    expect(orderService.getUserStats).toHaveBeenCalled();
    expect(component.stats).toEqual(mockStats);
    expect(component.statsLoading).toBeFalse();
  });

  it('should redirect to login when user is not logged in', () => {
    authService.getCurrentUser.and.returnValue(null);

    component.loadUserProfile();

    expect(snackBar.open).toHaveBeenCalledWith(
      'Utilisateur non connecté',
      'Erreur',
      jasmine.any(Object)
    );
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should handle stats loading error', () => {
    orderService.getUserStats.and.returnValue(throwError(() => new Error('Stats error')));

    component.loadUserStats();

    expect(component.statsLoading).toBeFalse();
    expect(snackBar.open).toHaveBeenCalledWith(
      'Erreur lors du chargement des statistiques',
      'Fermer',
      jasmine.any(Object)
    );
  });

  it('should return correct avatar URL', () => {
    component.user = mockUser;
    const avatarUrl = component.getAvatarUrl();

    expect(avatarUrl).toContain(mockUser.avatar);
  });

  it('should return empty string when no avatar', () => {
    component.user = { ...mockUser, avatar: undefined };
    const avatarUrl = component.getAvatarUrl();

    expect(avatarUrl).toBe('');
  });

  it('should return correct initials', () => {
    component.user = mockUser;
    expect(component.getInitials()).toBe('JD');

    component.user = { ...mockUser, name: 'Alice' };
    expect(component.getInitials()).toBe('A');

    component.user = null;
    expect(component.getInitials()).toBe('?');
  });

  it('should return correct role display', () => {
    component.user = mockUser;
    expect(component.getRoleDisplay()).toBe('Client');

    component.user = { ...mockUser, role: 'SELLER' };
    expect(component.getRoleDisplay()).toBe('Vendeur');

    component.user = null;
    expect(component.getRoleDisplay()).toBe('');
  });

  it('should return correct role icon', () => {
    component.user = mockUser;
    expect(component.getRoleIcon()).toBe('shopping_bag');

    component.user = { ...mockUser, role: 'SELLER' };
    expect(component.getRoleIcon()).toBe('storefront');

    component.user = null;
    expect(component.getRoleIcon()).toBe('person');
  });

  it('should format date correctly', () => {
    const date = new Date('2024-01-15');
    const formatted = component.formatDate(date);

    expect(formatted).toContain('2024');
    expect(formatted).toContain('janvier');
  });

  it('should return dash for undefined date', () => {
    expect(component.formatDate(undefined)).toBe('-');
  });

  it('should format currency correctly', () => {
    const formatted = component.formatCurrency(1500.50);

    expect(formatted).toContain('1');
    expect(formatted).toContain('500');
  });

  it('should navigate to orders', () => {
    component.goToOrders();
    expect(router.navigate).toHaveBeenCalledWith(['/orders']);
  });

  it('should navigate to products', () => {
    component.goToProducts();
    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });

  it('should navigate to cart', () => {
    component.goToCart();
    expect(router.navigate).toHaveBeenCalledWith(['/cart']);
  });

  it('should navigate to dashboard', () => {
    component.goToDashboard();
    expect(router.navigate).toHaveBeenCalledWith(['/seller/dashboard']);
  });

  it('should logout and navigate to login', () => {
    component.logout();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should update cart count from subscription', () => {
    (cartService.getCartCount as jasmine.Spy).and.returnValue(5);

    cartStream.next([{
      productId: '1',
      name: 'Test',
      price: 100,
      quantity: 5,
      imageUrl: null,
      sellerId: 'seller-1',
      sellerName: 'Test Seller'
    }]);

    expect(component.cartCount).toBe(5);
  });

  it('should check hasAvatar correctly', () => {
    component.user = mockUser;
    expect(component.hasAvatar()).toBeTrue();

    component.user = { ...mockUser, avatar: undefined };
    expect(component.hasAvatar()).toBeFalse();

    component.user = { ...mockUser, avatar: '' };
    expect(component.hasAvatar()).toBeFalse();
  });
});
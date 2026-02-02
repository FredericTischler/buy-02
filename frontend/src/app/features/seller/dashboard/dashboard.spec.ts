import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Dashboard } from './dashboard';
import { Product } from '../../../core/services/product';
import { Auth } from '../../../core/services/auth';
import { OrderService } from '../../../core/services/order';
import { MediaService } from '../../../core/services/media';
import { Product as ProductModel } from '../../../core/models/product.model';
import { SellerOrderStats, SellerProductStats } from '../../../core/models/order.model';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;
  let productService: jasmine.SpyObj<Product>;
  let authService: jasmine.SpyObj<Auth>;
  let orderService: jasmine.SpyObj<OrderService>;
  let mediaService: jasmine.SpyObj<MediaService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let router: Router;

  const mockProducts: ProductModel[] = [
    { id: 'p1', name: 'Product 1', description: 'Desc 1', price: 100, stock: 10, category: 'Electronics', sellerId: 's1', sellerName: 'Seller' },
    { id: 'p2', name: 'Product 2', description: 'Desc 2', price: 200, stock: 5, category: 'Fashion', sellerId: 's1', sellerName: 'Seller' }
  ];

  const mockStats: SellerOrderStats = {
    totalOrders: 50,
    completedOrders: 40,
    totalRevenue: 10000,
    totalItemsSold: 100
  };

  const mockProductStats: SellerProductStats = {
    bestSellingProducts: [
      { productId: 'p1', productName: 'Product 1', totalSold: 100, revenue: 10000, orderCount: 50 }
    ],
    recentSales: [],
    totalUniqueProductsSold: 10,
    totalCustomers: 25
  };

  const mockUser = { id: 'user-1', name: 'Test Seller', email: 'seller@test.com', role: 'SELLER' as const };

  beforeEach(async () => {
    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    productService = jasmine.createSpyObj<Product>('Product', [
      'getMyProducts',
      'deleteProduct'
    ]);
    productService.getMyProducts.and.returnValue(of(mockProducts));
    productService.deleteProduct.and.returnValue(of(undefined));

    authService = jasmine.createSpyObj<Auth>('Auth', ['getCurrentUser', 'logout']);
    authService.getCurrentUser.and.returnValue(mockUser);

    orderService = jasmine.createSpyObj<OrderService>('OrderService', [
      'getSellerStats',
      'getSellerProductStats'
    ]);
    orderService.getSellerStats.and.returnValue(of(mockStats));
    orderService.getSellerProductStats.and.returnValue(of(mockProductStats));

    mediaService = jasmine.createSpyObj<MediaService>('MediaService', ['getImageUrl']);
    mediaService.getImageUrl.and.callFake((url: string) => `https://localhost:8083${url}`);

    const dialogRefSpy = jasmine.createSpyObj<MatDialogRef<any>>('MatDialogRef', ['afterClosed']);
    dialogRefSpy.afterClosed.and.returnValue(of({ success: true }));

    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.returnValue(dialogRefSpy);

    await TestBed.configureTestingModule({
      imports: [
        Dashboard,
        RouterTestingModule.withRoutes([]),
        NoopAnimationsModule
      ],
      providers: [
        { provide: Product, useValue: productService },
        { provide: Auth, useValue: authService },
        { provide: OrderService, useValue: orderService },
        { provide: MediaService, useValue: mediaService },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snackBar }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load current user on init', () => {
    expect(authService.getCurrentUser).toHaveBeenCalled();
    expect(component.currentUser).toEqual(mockUser);
  });

  it('should load products on init', () => {
    expect(productService.getMyProducts).toHaveBeenCalled();
    expect(component.products).toEqual(mockProducts);
    expect(component.loading).toBeFalse();
  });

  it('should load stats on init', () => {
    expect(orderService.getSellerStats).toHaveBeenCalled();
    expect(component.stats).toEqual(mockStats);
    expect(component.statsLoading).toBeFalse();
  });

  it('should load product stats on init', () => {
    expect(orderService.getSellerProductStats).toHaveBeenCalled();
    expect(component.productStats).toEqual(mockProductStats);
    expect(component.productStatsLoading).toBeFalse();
  });

  it('should handle products loading error', fakeAsync(() => {
    productService.getMyProducts.and.returnValue(throwError(() => new Error('Error')));

    component.loadMyProducts();
    tick();

    expect(component.loading).toBeFalse();
    expect(component.errorMessage).toBe('Impossible de charger vos produits');
  }));

  it('should handle stats loading error', fakeAsync(() => {
    orderService.getSellerStats.and.returnValue(throwError(() => new Error('Error')));

    component.loadStats();
    tick();

    expect(component.statsLoading).toBeFalse();
  }));

  it('should handle product stats loading error', fakeAsync(() => {
    orderService.getSellerProductStats.and.returnValue(throwError(() => new Error('Error')));

    component.loadProductStats();
    tick();

    expect(component.productStatsLoading).toBeFalse();
  }));

  it('should open add product dialog', fakeAsync(() => {
    component.addProduct();
    tick();

    expect(dialog.open).toHaveBeenCalledWith(
      jasmine.any(Function),
      jasmine.objectContaining({ data: { mode: 'create' } })
    );
    expect(snackBar.open).toHaveBeenCalledWith(
      'Produit créé avec succès!',
      'Fermer',
      jasmine.any(Object)
    );
    expect(productService.getMyProducts).toHaveBeenCalled();
  }));

  it('should open edit product dialog', fakeAsync(() => {
    component.editProduct(mockProducts[0]);
    tick();

    expect(dialog.open).toHaveBeenCalledWith(
      jasmine.any(Function),
      jasmine.objectContaining({ data: { product: mockProducts[0], mode: 'edit' } })
    );
  }));

  it('should not reload products when dialog cancelled', fakeAsync(() => {
    const dialogRefSpy = jasmine.createSpyObj<MatDialogRef<any>>('MatDialogRef', ['afterClosed']);
    dialogRefSpy.afterClosed.and.returnValue(of({ success: false }));
    dialog.open.and.returnValue(dialogRefSpy);

    const initialCallCount = (productService.getMyProducts as jasmine.Spy).calls.count();

    component.addProduct();
    tick();

    expect((productService.getMyProducts as jasmine.Spy).calls.count()).toBe(initialCallCount);
  }));

  it('should delete product when confirmed', fakeAsync(() => {
    spyOn(window, 'confirm').and.returnValue(true);

    component.deleteProduct(mockProducts[0]);
    tick();

    expect(productService.deleteProduct).toHaveBeenCalledWith('p1');
    expect(snackBar.open).toHaveBeenCalledWith(
      'Produit supprimé avec succès!',
      'Fermer',
      jasmine.any(Object)
    );
  }));

  it('should not delete product when declined', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.deleteProduct(mockProducts[0]);

    expect(productService.deleteProduct).not.toHaveBeenCalled();
  });

  it('should handle delete product error', fakeAsync(() => {
    spyOn(window, 'confirm').and.returnValue(true);
    productService.deleteProduct.and.returnValue(throwError(() => new Error('Error')));

    component.deleteProduct(mockProducts[0]);
    tick();

    expect(snackBar.open).toHaveBeenCalledWith(
      'Erreur lors de la suppression',
      'Fermer',
      jasmine.objectContaining({ duration: 3000 })
    );
  }));

  it('should get product image url', () => {
    const url = component.getProductImageUrl('/api/media/file/p1/img.jpg');

    expect(mediaService.getImageUrl).toHaveBeenCalledWith('/api/media/file/p1/img.jpg');
    expect(url).toContain('https://localhost:8083');
  });

  it('should return empty string for undefined image url', () => {
    const url = component.getProductImageUrl(undefined);

    expect(url).toBe('');
  });

  it('should format date correctly', () => {
    const date = new Date('2024-01-15T10:30:00');
    const formatted = component.formatDate(date);

    expect(formatted).toContain('15');
  });

  it('should format currency correctly', () => {
    const formatted = component.formatCurrency(1234.56);

    expect(formatted).toContain('1');
    expect(formatted).toContain('234');
  });

  it('should navigate to products', () => {
    component.viewProducts();

    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });

  it('should navigate to orders', () => {
    component.viewOrders();

    expect(router.navigate).toHaveBeenCalledWith(['/seller/orders']);
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

  it('should have correct displayed columns', () => {
    expect(component.displayedColumns).toEqual(['name', 'category', 'price', 'stock', 'actions']);
  });
});
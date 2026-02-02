import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ProductDetail } from './product-detail';
import { Product as ProductService } from '../../../core/services/product';
import { MediaService } from '../../../core/services/media';
import { Cart } from '../../../core/services/cart';
import { Product as ProductModel } from '../../../core/models/product.model';
import { Media } from '../../../core/models/media.model';
import { CartItem } from '../../../core/models/cart.model';

describe('ProductDetail', () => {
  let component: ProductDetail;
  let fixture: ComponentFixture<ProductDetail>;
  let productService: jasmine.SpyObj<ProductService>;
  let mediaService: jasmine.SpyObj<MediaService>;
  let cartService: jasmine.SpyObj<Cart>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let router: Router;
  let route: ActivatedRoute;

  const mockProduct: ProductModel = {
    id: '1',
    name: 'Test Product',
    description: 'Test Description',
    price: 99.99,
    category: 'Tech',
    stock: 10,
    sellerId: 'seller1',
    sellerName: 'Test Seller',
  };

  const mockMedia: Media[] = [
    {
      id: 'm1',
      productId: '1',
      filename: 'image1.png',
      contentType: 'image/png',
      size: 1024,
      uploadedBy: 'seller1',
      url: '/media/image1.png',
      uploadedAt: new Date(),
    },
  ];

  beforeEach(async () => {
    productService = jasmine.createSpyObj<ProductService>('Product', ['getProductById']);
    mediaService = jasmine.createSpyObj<MediaService>('MediaService', ['getMediaByProduct', 'getImageUrl']);
    cartService = jasmine.createSpyObj<Cart>('Cart', ['getCartItems', 'addToCart']);
    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    productService.getProductById.and.returnValue(of(mockProduct));
    mediaService.getMediaByProduct.and.returnValue(of(mockMedia));
    mediaService.getImageUrl.and.callFake((url: string) => `http://cdn${url}`);
    cartService.getCartItems.and.returnValue([]);
    snackBar.open.and.returnValue({
      onAction: () => of(null),
    } as any);

    await TestBed.configureTestingModule({
      imports: [
        ProductDetail,
        RouterTestingModule.withRoutes([]),
        NoopAnimationsModule,
      ],
      providers: [
        { provide: ProductService, useValue: productService },
        { provide: MediaService, useValue: mediaService },
        { provide: Cart, useValue: cartService },
        { provide: MatSnackBar, useValue: snackBar },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'id' ? '1' : null),
              },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductDetail);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    route = TestBed.inject(ActivatedRoute);
    spyOn(router, 'navigate');
    (component as any).snackBar = snackBar;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load product on init', () => {
    fixture.detectChanges();

    expect(productService.getProductById).toHaveBeenCalledWith('1');
    expect(component.product).toEqual(mockProduct);
    expect(component.images).toEqual(['http://cdn/media/image1.png']);
    expect(component.loading).toBeFalse();
  });

  it('should handle missing product ID', () => {
    spyOn(route.snapshot.paramMap, 'get').and.returnValue(null);

    component.ngOnInit();

    expect(component.errorMessage).toBe('ID produit manquant');
    expect(component.loading).toBeFalse();
  });

  it('should handle product load error', () => {
    productService.getProductById.and.returnValue(throwError(() => new Error('Load error')));

    fixture.detectChanges();

    expect(component.errorMessage).toBe('Impossible de charger le produit');
    expect(component.loading).toBeFalse();
  });

  it('should handle media load error gracefully', () => {
    mediaService.getMediaByProduct.and.returnValue(throwError(() => new Error('Media error')));

    fixture.detectChanges();

    expect(component.product).toEqual(mockProduct);
    expect(component.images).toEqual([]);
  });

  it('should select image by index', () => {
    component.images = ['img1', 'img2', 'img3'];
    component.selectImage(2);

    expect(component.selectedImageIndex).toBe(2);
  });

  it('should navigate to previous image', () => {
    component.images = ['img1', 'img2', 'img3'];
    component.selectedImageIndex = 2;

    component.previousImage();

    expect(component.selectedImageIndex).toBe(1);
  });

  it('should not go before first image', () => {
    component.images = ['img1', 'img2'];
    component.selectedImageIndex = 0;

    component.previousImage();

    expect(component.selectedImageIndex).toBe(0);
  });

  it('should navigate to next image', () => {
    component.images = ['img1', 'img2', 'img3'];
    component.selectedImageIndex = 0;

    component.nextImage();

    expect(component.selectedImageIndex).toBe(1);
  });

  it('should not go beyond last image', () => {
    component.images = ['img1', 'img2'];
    component.selectedImageIndex = 1;

    component.nextImage();

    expect(component.selectedImageIndex).toBe(1);
  });

  it('should increase quantity when stock allows', () => {
    component.product = mockProduct;
    component.quantity = 5;

    component.increaseQuantity();

    expect(component.quantity).toBe(6);
  });

  it('should not increase quantity beyond stock', () => {
    component.product = mockProduct;
    component.quantity = 10;

    component.increaseQuantity();

    expect(component.quantity).toBe(10);
  });

  it('should decrease quantity when above 1', () => {
    component.quantity = 3;

    component.decreaseQuantity();

    expect(component.quantity).toBe(2);
  });

  it('should not decrease quantity below 1', () => {
    component.quantity = 1;

    component.decreaseQuantity();

    expect(component.quantity).toBe(1);
  });

  it('should add product to cart', () => {
    component.product = mockProduct;
    component.quantity = 2;
    component.images = ['http://cdn/image.png'];

    component.addToCart();

    expect(cartService.addToCart).toHaveBeenCalledWith({
      productId: '1',
      name: 'Test Product',
      price: 99.99,
      quantity: 2,
      imageUrl: 'http://cdn/image.png',
      stock: 10,
      sellerId: 'seller1',
      sellerName: 'Test Seller',
    });
    expect(component.quantity).toBe(1);
  });

  it('should not add to cart when product is null', () => {
    component.product = null;

    component.addToCart();

    expect(cartService.addToCart).not.toHaveBeenCalled();
  });

  it('should show error when adding beyond available stock', () => {
    component.product = mockProduct;
    component.quantity = 5;
    const existingItem: CartItem = {
      productId: '1',
      name: 'Test Product',
      price: 99.99,
      quantity: 8,
      imageUrl: null,
      sellerId: 'seller1',
      sellerName: 'Test Seller',
    };
    cartService.getCartItems.and.returnValue([existingItem]);

    component.addToCart();

    expect(cartService.addToCart).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('Stock insuffisant'),
      'Fermer',
      jasmine.any(Object)
    );
  });

  it('should show error when stock is already maxed in cart', () => {
    component.product = mockProduct;
    component.quantity = 1;
    const existingItem: CartItem = {
      productId: '1',
      name: 'Test Product',
      price: 99.99,
      quantity: 10,
      imageUrl: null,
      sellerId: 'seller1',
      sellerName: 'Test Seller',
    };
    cartService.getCartItems.and.returnValue([existingItem]);

    component.addToCart();

    expect(cartService.addToCart).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('Stock maximum déjà atteint'),
      'Fermer',
      jasmine.any(Object)
    );
  });

  it('should navigate to cart when snackbar action clicked', () => {
    const snackBarRef = {
      onAction: () => of(true),
    };
    snackBar.open.and.returnValue(snackBarRef as any);
    component.product = mockProduct;
    component.quantity = 1;

    component.addToCart();

    expect(snackBar.open).toHaveBeenCalled();
  });

  it('should navigate back to products', () => {
    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });

  it('should return correct stock status', () => {
    component.product = { ...mockProduct, stock: 0 };
    expect(component.stockStatus).toBe('Rupture de stock');

    component.product = { ...mockProduct, stock: 5 };
    expect(component.stockStatus).toBe('Stock limité');

    component.product = { ...mockProduct, stock: 15 };
    expect(component.stockStatus).toBe('En stock');

    component.product = null;
    expect(component.stockStatus).toBe('');
  });

  it('should return correct stock status class', () => {
    component.product = { ...mockProduct, stock: 0 };
    expect(component.stockStatusClass).toBe('out-of-stock');

    component.product = { ...mockProduct, stock: 5 };
    expect(component.stockStatusClass).toBe('low-stock');

    component.product = { ...mockProduct, stock: 15 };
    expect(component.stockStatusClass).toBe('in-stock');

    component.product = null;
    expect(component.stockStatusClass).toBe('');
  });
});

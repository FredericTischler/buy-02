import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MatSnackBar, MatSnackBarRef, TextOnlySnackBar } from '@angular/material/snack-bar';

import { WishlistPage } from './wishlist';
import { WishlistService } from '../../core/services/wishlist';
import { Product } from '../../core/services/product';
import { MediaService } from '../../core/services/media';
import { Cart } from '../../core/services/cart';

describe('WishlistPage', () => {
  let component: WishlistPage;
  let fixture: ComponentFixture<WishlistPage>;
  let wishlistService: jasmine.SpyObj<WishlistService>;
  let productService: jasmine.SpyObj<Product>;
  let mediaService: jasmine.SpyObj<MediaService>;
  let cartService: jasmine.SpyObj<Cart>;
  let router: Router;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  const mockProducts = [
    { id: 'p1', name: 'Product 1', price: 100, stock: 10, sellerId: 's1', sellerName: 'Seller 1' },
    { id: 'p2', name: 'Product 2', price: 200, stock: 5, sellerId: 's2', sellerName: 'Seller 2' }
  ];

  const mockMedia = [{ id: 'm1', url: '/api/media/file/p1/img.jpg' }];

  beforeEach(async () => {
    const snackBarRefSpy = jasmine.createSpyObj<MatSnackBarRef<TextOnlySnackBar>>('MatSnackBarRef', ['onAction']);
    snackBarRefSpy.onAction.and.returnValue(of(undefined));

    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    snackBar.open.and.returnValue(snackBarRefSpy);

    wishlistService = jasmine.createSpyObj<WishlistService>('WishlistService', [
      'getWishlistIds',
      'removeFromWishlist',
      'clearWishlist'
    ]);
    wishlistService.getWishlistIds.and.returnValue(of(['p1', 'p2']));
    wishlistService.removeFromWishlist.and.returnValue(of({ message: 'Removed', wishlist: ['p2'] }));
    wishlistService.clearWishlist.and.returnValue(of({ message: 'Cleared' }));

    productService = jasmine.createSpyObj<Product>('Product', ['getProductById']);
    productService.getProductById.and.callFake((id: string) => {
      const product = mockProducts.find(p => p.id === id);
      return product ? of(product as any) : throwError(() => new Error('Not found'));
    });

    mediaService = jasmine.createSpyObj<MediaService>('MediaService', ['getMediaByProduct', 'getImageUrl']);
    mediaService.getMediaByProduct.and.returnValue(of(mockMedia as any));
    mediaService.getImageUrl.and.callFake((url: string) => `https://localhost:8083${url}`);

    cartService = jasmine.createSpyObj<Cart>('Cart', ['getCartItems', 'addToCart']);
    cartService.getCartItems.and.returnValue([]);

    await TestBed.configureTestingModule({
      imports: [
        WishlistPage,
        RouterTestingModule.withRoutes([]),
        NoopAnimationsModule
      ],
      providers: [
        { provide: WishlistService, useValue: wishlistService },
        { provide: Product, useValue: productService },
        { provide: MediaService, useValue: mediaService },
        { provide: Cart, useValue: cartService },
        { provide: MatSnackBar, useValue: snackBar }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WishlistPage);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load wishlist on init', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    expect(wishlistService.getWishlistIds).toHaveBeenCalled();
    expect(productService.getProductById).toHaveBeenCalledWith('p1');
    expect(productService.getProductById).toHaveBeenCalledWith('p2');
    expect(component.products.length).toBe(2);
    expect(component.loading).toBeFalse();
  }));

  it('should handle empty wishlist', fakeAsync(() => {
    wishlistService.getWishlistIds.and.returnValue(of([]));

    fixture.detectChanges();
    tick();

    expect(component.products).toEqual([]);
    expect(component.loading).toBeFalse();
  }));

  it('should handle wishlist loading error', fakeAsync(() => {
    wishlistService.getWishlistIds.and.returnValue(throwError(() => new Error('Network error')));

    fixture.detectChanges();
    tick();

    expect(component.errorMessage).toBe('Impossible de charger la wishlist');
    expect(component.loading).toBeFalse();
  }));

  it('should remove product from wishlist', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    const product = { id: 'p1', name: 'Product 1' };
    component.removeFromWishlist(product);
    tick();

    expect(wishlistService.removeFromWishlist).toHaveBeenCalledWith('p1');
    expect(snackBar.open).toHaveBeenCalledWith(
      'Product 1 retiré de la wishlist',
      'Fermer',
      jasmine.objectContaining({ duration: 2000 })
    );
  }));

  it('should handle remove from wishlist error', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    wishlistService.removeFromWishlist.and.returnValue(throwError(() => new Error('Error')));

    component.removeFromWishlist({ id: 'p1', name: 'Product 1' });
    tick();

    expect(snackBar.open).toHaveBeenCalledWith(
      'Erreur lors de la suppression',
      'Fermer',
      jasmine.objectContaining({ duration: 3000 })
    );
  }));

  it('should add product to cart', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    const product = {
      id: 'p1',
      name: 'Product 1',
      price: 100,
      stock: 10,
      sellerId: 's1',
      sellerName: 'Seller 1',
      imageUrl: '/img.jpg'
    };

    component.addToCart(product);

    expect(cartService.addToCart).toHaveBeenCalledWith(jasmine.objectContaining({
      productId: 'p1',
      name: 'Product 1',
      price: 100,
      quantity: 1
    }));
    expect(snackBar.open).toHaveBeenCalledWith(
      'Product 1 ajouté au panier',
      'Voir le panier',
      jasmine.any(Object)
    );
  }));

  it('should not add out of stock product to cart', () => {
    const product = { id: 'p1', name: 'Product 1', stock: 0 };

    component.addToCart(product);

    expect(cartService.addToCart).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith(
      'Produit en rupture de stock',
      'Fermer',
      jasmine.objectContaining({ duration: 2000 })
    );
  });

  it('should not add product when cart already at max stock', () => {
    cartService.getCartItems.and.returnValue([{ productId: 'p1', quantity: 10 }] as any);

    const product = { id: 'p1', name: 'Product 1', stock: 10 };

    component.addToCart(product);

    expect(cartService.addToCart).not.toHaveBeenCalled();
  });

  it('should navigate to product details', () => {
    component.viewDetails('p1');

    expect(router.navigate).toHaveBeenCalledWith(['/products', 'p1']);
  });

  it('should clear wishlist when confirmed', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    spyOn(window, 'confirm').and.returnValue(true);

    component.clearWishlist();
    tick();

    expect(wishlistService.clearWishlist).toHaveBeenCalled();
    expect(component.products).toEqual([]);
  }));

  it('should not clear wishlist when declined', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.clearWishlist();

    expect(wishlistService.clearWishlist).not.toHaveBeenCalled();
  });

  it('should handle clear wishlist error', fakeAsync(() => {
    spyOn(window, 'confirm').and.returnValue(true);
    wishlistService.clearWishlist.and.returnValue(throwError(() => new Error('Error')));

    component.clearWishlist();
    tick();

    expect(wishlistService.clearWishlist).toHaveBeenCalled();
  }));

  it('should navigate back to products', () => {
    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });

  it('should navigate to cart', () => {
    component.goToCart();

    expect(router.navigate).toHaveBeenCalledWith(['/cart']);
  });

  it('should unsubscribe on destroy', () => {
    fixture.detectChanges();
    component.ngOnDestroy();

    expect(component).toBeTruthy();
  });
});
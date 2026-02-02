import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CartPage } from './cart';
import { Cart } from '../../core/services/cart';
import { CartItem } from '../../core/models/cart.model';

describe('CartPage', () => {
  let component: CartPage;
  let fixture: ComponentFixture<CartPage>;
  let cartService: Cart;
  let router: Router;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let cartStream: BehaviorSubject<CartItem[]>;

  beforeEach(async () => {
    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    cartStream = new BehaviorSubject<CartItem[]>([]);

    cartService = {
      cartItems$: cartStream.asObservable(),
      getCartItems: jasmine.createSpy('getCartItems').and.returnValue([]),
      updateQuantity: jasmine.createSpy('updateQuantity'),
      removeFromCart: jasmine.createSpy('removeFromCart'),
      clearCart: jasmine.createSpy('clearCart'),
      getCartTotal: jasmine.createSpy('getCartTotal').and.returnValue(0),
    } as unknown as Cart;

    await TestBed.configureTestingModule({
      imports: [
        CartPage,
        RouterTestingModule.withRoutes([]),
        NoopAnimationsModule,
      ],
      providers: [
        { provide: Cart, useValue: cartService },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CartPage);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    (component as any).snackBar = snackBar;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load cart items when loading cart', () => {
    const items: CartItem[] = [
      { productId: '1', name: 'Product 1', price: 10, quantity: 2, imageUrl: null, sellerId: 'seller-1', sellerName: 'Test Seller' },
    ];
    (cartService.getCartItems as jasmine.Spy).and.returnValue(items);

    component.loadCart();

    expect(cartService.getCartItems).toHaveBeenCalled();
    expect(component.cartItems).toEqual(items);
  });

  it('should update cart items from subscription', () => {
    const items: CartItem[] = [
      { productId: '1', name: 'Product 1', price: 10, quantity: 2, imageUrl: null, sellerId: 'seller-1', sellerName: 'Test Seller' },
    ];

    cartStream.next(items);

    expect(component.cartItems).toEqual(items);
  });

  it('should increase quantity when stock allows', () => {
    const item: CartItem = {
      productId: '1',
      name: 'Product 1',
      price: 10,
      quantity: 2,
      stock: 5,
      imageUrl: null,
      sellerId: 'seller-1',
      sellerName: 'Test Seller',
    };

    component.increaseQuantity(item);

    expect(cartService.updateQuantity).toHaveBeenCalledWith('1', 3);
    expect(snackBar.open).not.toHaveBeenCalled();
  });

  it('should show error when trying to increase beyond stock', () => {
    const item: CartItem = {
      productId: '1',
      name: 'Product 1',
      price: 10,
      quantity: 5,
      stock: 5,
      imageUrl: null,
      sellerId: 'seller-1',
      sellerName: 'Test Seller',
    };

    component.increaseQuantity(item);

    expect(cartService.updateQuantity).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith(
      'Stock maximum atteint (5 disponibles)',
      'Fermer',
      jasmine.objectContaining({ duration: 3000 })
    );
  });

  it('should allow unlimited quantity when stock is undefined', () => {
    const item: CartItem = {
      productId: '1',
      name: 'Product 1',
      price: 10,
      quantity: 100,
      imageUrl: null,
      sellerId: 'seller-1',
      sellerName: 'Test Seller',
    };

    component.increaseQuantity(item);

    expect(cartService.updateQuantity).toHaveBeenCalledWith('1', 101);
  });

  it('should decrease quantity when quantity > 1', () => {
    const item: CartItem = {
      productId: '1',
      name: 'Product 1',
      price: 10,
      quantity: 3,
      imageUrl: null,
      sellerId: 'seller-1',
      sellerName: 'Test Seller',
    };

    component.decreaseQuantity(item);

    expect(cartService.updateQuantity).toHaveBeenCalledWith('1', 2);
  });

  it('should not decrease quantity when quantity is 1', () => {
    const item: CartItem = {
      productId: '1',
      name: 'Product 1',
      price: 10,
      quantity: 1,
      imageUrl: null,
      sellerId: 'seller-1',
      sellerName: 'Test Seller',
    };

    component.decreaseQuantity(item);

    expect(cartService.updateQuantity).not.toHaveBeenCalled();
  });

  it('should remove item from cart', () => {
    const item: CartItem = {
      productId: '1',
      name: 'Product 1',
      price: 10,
      quantity: 2,
      imageUrl: null,
      sellerId: 'seller-1',
      sellerName: 'Test Seller',
    };

    component.removeItem(item);

    expect(cartService.removeFromCart).toHaveBeenCalledWith('1');
    expect(snackBar.open).toHaveBeenCalledWith(
      'Product 1 retiré du panier',
      'Fermer',
      jasmine.objectContaining({ duration: 2000 })
    );
  });

  it('should clear cart when confirmed', () => {
    spyOn(window, 'confirm').and.returnValue(true);

    component.clearCart();

    expect(cartService.clearCart).toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith(
      'Panier vidé',
      'Fermer',
      jasmine.objectContaining({ duration: 2000 })
    );
  });

  it('should not clear cart when cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.clearCart();

    expect(cartService.clearCart).not.toHaveBeenCalled();
  });

  it('should calculate total from cart service', () => {
    (cartService.getCartTotal as jasmine.Spy).and.returnValue(150);

    const total = component.getTotal();

    expect(total).toBe(150);
    expect(cartService.getCartTotal).toHaveBeenCalled();
  });

  it('should calculate item total', () => {
    const item: CartItem = {
      productId: '1',
      name: 'Product 1',
      price: 25,
      quantity: 3,
      imageUrl: null,
      sellerId: 'seller-1',
      sellerName: 'Test Seller',
    };

    const itemTotal = component.getItemTotal(item);

    expect(itemTotal).toBe(75);
  });

  it('should navigate to products when continuing shopping', () => {
    component.continueShopping();

    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });

  it('should navigate to products when going back', () => {
    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });

  it('should show message when checking out with empty cart', () => {
    component.cartItems = [];

    component.checkout();

    expect(snackBar.open).toHaveBeenCalledWith(
      'Votre panier est vide',
      'Fermer',
      jasmine.objectContaining({ duration: 3000 })
    );
    expect(router.navigate).not.toHaveBeenCalledWith(['/checkout']);
  });

  it('should navigate to checkout when cart has items', () => {
    component.cartItems = [
      { productId: '1', name: 'Product 1', price: 10, quantity: 2, imageUrl: null, sellerId: 'seller-1', sellerName: 'Test Seller' },
    ];

    component.checkout();

    expect(router.navigate).toHaveBeenCalledWith(['/checkout']);
  });
});

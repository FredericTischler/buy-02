import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Cart } from './cart';
import { CartItem } from '../models/cart.model';

describe('Cart service', () => {
  let service: Cart;
  let httpMock: HttpTestingController;

  const sampleItem: CartItem = {
    productId: 'p1',
    name: 'Phone',
    price: 100,
    quantity: 1,
    imageUrl: '/img.png',
    sellerId: 'seller-1',
    sellerName: 'Test Seller',
  };

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('current_user', JSON.stringify({ id: 'user-1' }));

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [Cart],
    });

    service = TestBed.inject(Cart);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should add and persist items locally', () => {
    service.addToCart(sampleItem);

    // Flush the backend POST request
    const req = httpMock.expectOne(r => r.url.includes('/api/cart/items'));
    req.flush({ id: 'cart-1', userId: 'user-1', items: [{ ...sampleItem, productName: 'Phone' }], totalAmount: 100, totalItems: 1 });

    expect(service.getCartItems().length).toBe(1);
    expect(service.getCartCount()).toBe(1);
    expect(service.getCartTotal()).toBe(100);
    const stored = JSON.parse(localStorage.getItem('cart_user-1')!);
    expect(stored[0].productId).toBe('p1');
  });

  it('should merge quantities when adding existing item', () => {
    service.addToCart(sampleItem);
    const req1 = httpMock.expectOne(r => r.url.includes('/api/cart/items'));
    req1.flush({ id: 'cart-1', userId: 'user-1', items: [{ ...sampleItem, productName: 'Phone' }], totalAmount: 100, totalItems: 1 });

    service.addToCart({ ...sampleItem, quantity: 2 });
    const req2 = httpMock.expectOne(r => r.url.includes('/api/cart/items'));
    req2.flush({ id: 'cart-1', userId: 'user-1', items: [{ ...sampleItem, productName: 'Phone', quantity: 3 }], totalAmount: 300, totalItems: 3 });

    expect(service.getCartItems()[0].quantity).toBe(3);
  });

  it('should update quantity and remove when zero', () => {
    service.addToCart(sampleItem);
    const addReq = httpMock.expectOne(r => r.url.includes('/api/cart/items'));
    addReq.flush({ id: 'cart-1', userId: 'user-1', items: [{ ...sampleItem, productName: 'Phone' }], totalAmount: 100, totalItems: 1 });

    service.updateQuantity('p1', 5);
    const putReq = httpMock.expectOne(r => r.method === 'PUT' && r.url.includes('/api/cart/items/p1'));
    putReq.flush({ id: 'cart-1', userId: 'user-1', items: [{ ...sampleItem, productName: 'Phone', quantity: 5 }], totalAmount: 500, totalItems: 5 });
    expect(service.getCartItems()[0].quantity).toBe(5);

    service.updateQuantity('p1', 0);
    const delReq = httpMock.expectOne(r => r.method === 'DELETE' && r.url.includes('/api/cart/items/p1'));
    delReq.flush({ id: 'cart-1', userId: 'user-1', items: [], totalAmount: 0, totalItems: 0 });
    expect(service.getCartItems().length).toBe(0);
  });

  it('should remove specific product', () => {
    service.addToCart(sampleItem);
    const addReq1 = httpMock.expectOne(r => r.url.includes('/api/cart/items'));
    addReq1.flush({ id: 'cart-1', userId: 'user-1', items: [{ ...sampleItem, productName: 'Phone' }], totalAmount: 100, totalItems: 1 });

    service.addToCart({ ...sampleItem, productId: 'p2', name: 'Tablet' });
    const addReq2 = httpMock.expectOne(r => r.url.includes('/api/cart/items'));
    addReq2.flush({ id: 'cart-1', userId: 'user-1', items: [
      { ...sampleItem, productName: 'Phone' },
      { ...sampleItem, productId: 'p2', productName: 'Tablet' }
    ], totalAmount: 200, totalItems: 2 });

    service.removeFromCart('p1');
    const delReq = httpMock.expectOne(r => r.method === 'DELETE' && r.url.includes('/api/cart/items/p1'));
    delReq.flush({ id: 'cart-1', userId: 'user-1', items: [
      { ...sampleItem, productId: 'p2', productName: 'Tablet' }
    ], totalAmount: 100, totalItems: 1 });

    expect(service.getCartItems().map(i => i.productId)).toEqual(['p2']);
  });

  it('should clear cart', () => {
    service.addToCart(sampleItem);
    const addReq = httpMock.expectOne(r => r.url.includes('/api/cart/items'));
    addReq.flush({ id: 'cart-1', userId: 'user-1', items: [{ ...sampleItem, productName: 'Phone' }], totalAmount: 100, totalItems: 1 });

    service.clearCart();
    const delReq = httpMock.expectOne(r => r.method === 'DELETE' && r.url.includes('/api/cart'));
    delReq.flush({ id: 'cart-1', userId: 'user-1', items: [], totalAmount: 0, totalItems: 0 });

    expect(service.getCartItems()).toEqual([]);
  });

  it('should clear on logout', () => {
    service.addToCart(sampleItem);
    const addReq = httpMock.expectOne(r => r.url.includes('/api/cart/items'));
    addReq.flush({ id: 'cart-1', userId: 'user-1', items: [{ ...sampleItem, productName: 'Phone' }], totalAmount: 100, totalItems: 1 });

    service.clearCartOnLogout();
    expect(service.getCartItems()).toEqual([]);
  });

  it('should load cart from backend', () => {
    service.loadCart();
    const getReq = httpMock.expectOne(r => r.method === 'GET' && r.url.includes('/api/cart'));
    getReq.flush({
      id: 'cart-1', userId: 'user-1',
      items: [{ productId: 'p1', productName: 'Phone', price: 100, quantity: 2, imageUrl: null, sellerId: 'seller-1', sellerName: 'Seller' }],
      totalAmount: 200, totalItems: 2
    });

    expect(service.getCartItems().length).toBe(1);
    expect(service.getCartItems()[0].name).toBe('Phone');
  });

  it('should fallback to localStorage when backend fails on load', () => {
    localStorage.setItem('cart_user-1', JSON.stringify([sampleItem]));

    service.loadCart();
    const getReq = httpMock.expectOne(r => r.method === 'GET' && r.url.includes('/api/cart'));
    getReq.error(new ProgressEvent('error'));

    expect(service.getCartItems().length).toBe(1);
  });
});
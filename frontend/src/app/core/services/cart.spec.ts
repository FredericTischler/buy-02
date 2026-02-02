import { Cart } from './cart';
import { CartItem } from '../models/cart.model';

describe('Cart service', () => {
  let service: Cart;

  const sampleItem: CartItem = {
    productId: 'p1',
    name: 'Phone',
    price: 100,
    quantity: 1,
    imageUrl: '/img.png',
  };

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('current_user', JSON.stringify({ id: 'user-1' }));
    service = new Cart();
  });

  it('should add and persist items', () => {
    service.addToCart(sampleItem);

    expect(service.getCartItems().length).toBe(1);
    expect(service.getCartCount()).toBe(1);
    expect(service.getCartTotal()).toBe(100);
    const stored = JSON.parse(localStorage.getItem('cart_user-1')!);
    expect(stored[0].productId).toBe('p1');
  });

  it('should merge quantities when adding existing item', () => {
    service.addToCart(sampleItem);
    service.addToCart({ ...sampleItem, quantity: 2 });

    expect(service.getCartItems()[0].quantity).toBe(3);
  });

  it('should update quantity and remove when zero', () => {
    service.addToCart(sampleItem);
    service.updateQuantity('p1', 5);
    expect(service.getCartItems()[0].quantity).toBe(5);

    service.updateQuantity('p1', 0);
    expect(service.getCartItems().length).toBe(0);
  });

  it('should remove specific product', () => {
    service.addToCart(sampleItem);
    service.addToCart({ ...sampleItem, productId: 'p2' });

    service.removeFromCart('p1');
    expect(service.getCartItems().map(i => i.productId)).toEqual(['p2']);
  });

  it('should clear cart and reset subject', () => {
    service.addToCart(sampleItem);
    service.clearCart();
    expect(service.getCartItems()).toEqual([]);
  });

  it('should reload cart from storage and clear on logout', () => {
    service.addToCart(sampleItem);
    const otherService = new Cart();
    otherService.loadCart();
    expect(otherService.getCartItems().length).toBe(1);

    otherService.clearCartOnLogout();
    expect(otherService.getCartItems()).toEqual([]);
  });
});

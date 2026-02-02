import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { OrderService } from './order';
import {
  Order,
  OrderRequest,
  OrderStatus,
  StatusUpdateRequest,
  UserOrderStats,
  SellerOrderStats,
  UserProductStats,
  SellerProductStats,
  OrderSearchParams
} from '../models/order.model';

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;

  const mockOrder: Order = {
    id: 'order-1',
    userId: 'user-1',
    userName: 'Test User',
    userEmail: 'test@test.com',
    items: [
      { productId: 'p1', productName: 'Product 1', sellerId: 's1', sellerName: 'Seller', price: 100, quantity: 2 }
    ],
    totalAmount: 200,
    status: OrderStatus.PENDING,
    shippingAddress: '123 Test St',
    shippingCity: 'Paris',
    shippingPostalCode: '75001',
    shippingCountry: 'France',
    phoneNumber: '0612345678',
    paymentMethod: 'COD',
    createdAt: new Date()
  };

  const mockOrderRequest: OrderRequest = {
    items: [
      { productId: 'p1', productName: 'Product 1', sellerId: 's1', sellerName: 'Seller', price: 100, quantity: 2 }
    ],
    shippingAddress: '123 Test St',
    shippingCity: 'Paris',
    shippingPostalCode: '75001',
    shippingCountry: 'France',
    phoneNumber: '0612345678',
    paymentMethod: 'COD'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [OrderService]
    });

    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('createOrder', () => {
    it('should create an order', () => {
      service.createOrder(mockOrderRequest).subscribe(order => {
        expect(order).toEqual(mockOrder);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/orders') && req.method === 'POST');
      expect(req.request.body).toEqual(mockOrderRequest);
      req.flush(mockOrder);
    });
  });

  describe('getOrderById', () => {
    it('should get order by id', () => {
      service.getOrderById('order-1').subscribe(order => {
        expect(order).toEqual(mockOrder);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/orders/order-1'));
      expect(req.request.method).toBe('GET');
      req.flush(mockOrder);
    });
  });

  describe('getMyOrders', () => {
    it('should get all orders for user', () => {
      service.getMyOrders().subscribe(orders => {
        expect(orders).toEqual([mockOrder]);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/orders/my-orders') && !req.url.includes('status'));
      expect(req.request.method).toBe('GET');
      req.flush([mockOrder]);
    });

    it('should get orders filtered by status', () => {
      service.getMyOrders(OrderStatus.PENDING).subscribe(orders => {
        expect(orders).toEqual([mockOrder]);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/orders/my-orders?status=PENDING'));
      expect(req.request.method).toBe('GET');
      req.flush([mockOrder]);
    });
  });

  describe('searchMyOrders', () => {
    it('should search orders with params', () => {
      const params: OrderSearchParams = {
        keyword: 'test',
        status: OrderStatus.PENDING,
        sortBy: 'createdAt',
        sortDir: 'desc'
      };

      service.searchMyOrders(params).subscribe(orders => {
        expect(orders).toEqual([mockOrder]);
      });

      const req = httpMock.expectOne(req =>
        req.url.includes('/api/orders/my-orders/search') &&
        req.url.includes('keyword=test') &&
        req.url.includes('status=PENDING') &&
        req.url.includes('sortBy=createdAt') &&
        req.url.includes('sortDir=desc')
      );
      expect(req.request.method).toBe('GET');
      req.flush([mockOrder]);
    });

    it('should search orders without optional params', () => {
      const params: OrderSearchParams = {};

      service.searchMyOrders(params).subscribe(orders => {
        expect(orders).toEqual([mockOrder]);
      });

      const req = httpMock.expectOne(req =>
        req.url.includes('/api/orders/my-orders/search') &&
        !req.url.includes('?')
      );
      expect(req.request.method).toBe('GET');
      req.flush([mockOrder]);
    });
  });

  describe('getSellerOrders', () => {
    it('should get all orders for seller', () => {
      service.getSellerOrders().subscribe(orders => {
        expect(orders).toEqual([mockOrder]);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/orders/seller') && !req.url.includes('status'));
      expect(req.request.method).toBe('GET');
      req.flush([mockOrder]);
    });

    it('should get seller orders filtered by status', () => {
      service.getSellerOrders(OrderStatus.SHIPPED).subscribe(orders => {
        expect(orders).toEqual([mockOrder]);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/orders/seller?status=SHIPPED'));
      expect(req.request.method).toBe('GET');
      req.flush([mockOrder]);
    });
  });

  describe('searchSellerOrders', () => {
    it('should search seller orders with params', () => {
      const params: OrderSearchParams = {
        keyword: 'product',
        sortBy: 'totalAmount',
        sortDir: 'asc'
      };

      service.searchSellerOrders(params).subscribe(orders => {
        expect(orders).toEqual([mockOrder]);
      });

      const req = httpMock.expectOne(req =>
        req.url.includes('/api/orders/seller/search') &&
        req.url.includes('keyword=product')
      );
      expect(req.request.method).toBe('GET');
      req.flush([mockOrder]);
    });
  });

  describe('updateOrderStatus', () => {
    it('should update order status', () => {
      const request: StatusUpdateRequest = {
        status: OrderStatus.CONFIRMED
      };
      const updatedOrder = { ...mockOrder, status: OrderStatus.CONFIRMED };

      service.updateOrderStatus('order-1', request).subscribe(order => {
        expect(order.status).toBe(OrderStatus.CONFIRMED);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/orders/order-1/status'));
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(request);
      req.flush(updatedOrder);
    });
  });

  describe('cancelOrder', () => {
    it('should cancel order with reason', () => {
      const cancelledOrder = { ...mockOrder, status: OrderStatus.CANCELLED };

      service.cancelOrder('order-1', 'Changed my mind').subscribe(order => {
        expect(order.status).toBe(OrderStatus.CANCELLED);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/orders/order-1/cancel'));
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ reason: 'Changed my mind' });
      req.flush(cancelledOrder);
    });

    it('should cancel order without reason', () => {
      const cancelledOrder = { ...mockOrder, status: OrderStatus.CANCELLED };

      service.cancelOrder('order-1').subscribe(order => {
        expect(order.status).toBe(OrderStatus.CANCELLED);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/orders/order-1/cancel'));
      expect(req.request.body).toEqual({ reason: undefined });
      req.flush(cancelledOrder);
    });
  });

  describe('reorder', () => {
    it('should create new order from existing', () => {
      const newOrder = { ...mockOrder, id: 'order-2' };

      service.reorder('order-1').subscribe(order => {
        expect(order.id).toBe('order-2');
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/orders/order-1/reorder'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(newOrder);
    });
  });

  describe('getUserStats', () => {
    it('should get user order statistics', () => {
      const stats: UserOrderStats = {
        totalOrders: 10,
        completedOrders: 7,
        totalSpent: 1500
      };

      service.getUserStats().subscribe(result => {
        expect(result).toEqual(stats);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/orders/stats/user'));
      expect(req.request.method).toBe('GET');
      req.flush(stats);
    });
  });

  describe('getSellerStats', () => {
    it('should get seller order statistics', () => {
      const stats: SellerOrderStats = {
        totalOrders: 50,
        completedOrders: 40,
        totalRevenue: 10000,
        totalItemsSold: 100
      };

      service.getSellerStats().subscribe(result => {
        expect(result).toEqual(stats);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/orders/stats/seller'));
      expect(req.request.method).toBe('GET');
      req.flush(stats);
    });
  });

  describe('getUserProductStats', () => {
    it('should get user product statistics', () => {
      const stats: UserProductStats = {
        mostPurchasedProducts: [
          { productId: 'p1', productName: 'Product 1', sellerId: 's1', sellerName: 'Seller', totalQuantity: 10, totalSpent: 1000, lastPurchased: new Date() }
        ],
        topCategories: [
          { category: 'Electronics', orderCount: 5, itemCount: 20, totalSpent: 2000 }
        ],
        totalUniqueProducts: 5,
        totalItemsPurchased: 20
      };

      service.getUserProductStats().subscribe(result => {
        expect(result).toEqual(stats);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/orders/stats/user/products'));
      expect(req.request.method).toBe('GET');
      req.flush(stats);
    });
  });

  describe('getSellerProductStats', () => {
    it('should get seller product statistics', () => {
      const stats: SellerProductStats = {
        bestSellingProducts: [
          { productId: 'p1', productName: 'Product 1', totalSold: 100, revenue: 10000, orderCount: 50 }
        ],
        recentSales: [],
        totalUniqueProductsSold: 10,
        totalCustomers: 25
      };

      service.getSellerProductStats().subscribe(result => {
        expect(result).toEqual(stats);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/orders/stats/seller/products'));
      expect(req.request.method).toBe('GET');
      req.flush(stats);
    });
  });

  describe('getStatusDisplay', () => {
    it('should return correct display text for each status', () => {
      expect(service.getStatusDisplay(OrderStatus.PENDING)).toBe('En attente');
      expect(service.getStatusDisplay(OrderStatus.CONFIRMED)).toBe('Confirmée');
      expect(service.getStatusDisplay(OrderStatus.PROCESSING)).toBe('En préparation');
      expect(service.getStatusDisplay(OrderStatus.SHIPPED)).toBe('Expédiée');
      expect(service.getStatusDisplay(OrderStatus.DELIVERED)).toBe('Livrée');
      expect(service.getStatusDisplay(OrderStatus.CANCELLED)).toBe('Annulée');
      expect(service.getStatusDisplay(OrderStatus.REFUNDED)).toBe('Remboursée');
    });
  });

  describe('getStatusColor', () => {
    it('should return correct color for each status', () => {
      expect(service.getStatusColor(OrderStatus.PENDING)).toBe('warn');
      expect(service.getStatusColor(OrderStatus.CONFIRMED)).toBe('primary');
      expect(service.getStatusColor(OrderStatus.PROCESSING)).toBe('primary');
      expect(service.getStatusColor(OrderStatus.SHIPPED)).toBe('accent');
      expect(service.getStatusColor(OrderStatus.DELIVERED)).toBe('primary');
      expect(service.getStatusColor(OrderStatus.CANCELLED)).toBe('warn');
      expect(service.getStatusColor(OrderStatus.REFUNDED)).toBe('warn');
    });
  });
});
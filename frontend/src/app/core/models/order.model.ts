export interface OrderItem {
  productId: string;
  productName: string;
  sellerId: string;
  sellerName?: string;
  price: number;
  quantity: number;
  imageUrl?: string;
}

export interface OrderItemRequest {
  productId: string;
  productName: string;
  sellerId: string;
  sellerName?: string;
  price: number;
  quantity: number;
  imageUrl?: string;
}

export interface OrderRequest {
  items: OrderItemRequest[];
  shippingAddress: string;
  shippingCity: string;
  shippingPostalCode: string;
  shippingCountry: string;
  phoneNumber: string;
  paymentMethod?: string;
  notes?: string;
}

export interface Order {
  id: string;
  userId: string;
  userName: string;
  userEmail: string;
  items: OrderItem[];
  totalAmount: number;
  status: OrderStatus;
  shippingAddress: string;
  shippingCity: string;
  shippingPostalCode: string;
  shippingCountry: string;
  phoneNumber: string;
  paymentMethod: string;
  notes?: string;
  createdAt: Date;
  updatedAt?: Date;
  confirmedAt?: Date;
  shippedAt?: Date;
  deliveredAt?: Date;
  cancelledAt?: Date;
  cancellationReason?: string;
}

export enum OrderStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  PROCESSING = 'PROCESSING',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED',
  REFUNDED = 'REFUNDED'
}

export interface StatusUpdateRequest {
  status: OrderStatus;
  reason?: string;
}

export interface UserOrderStats {
  totalOrders: number;
  completedOrders: number;
  totalSpent: number;
}

export interface SellerOrderStats {
  totalOrders: number;
  completedOrders: number;
  totalRevenue: number;
  totalItemsSold: number;
}
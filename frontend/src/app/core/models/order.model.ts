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

// Advanced stats interfaces
export interface ProductPurchaseInfo {
  productId: string;
  productName: string;
  sellerId: string;
  sellerName: string;
  totalQuantity: number;
  totalSpent: number;
  lastPurchased: Date;
  imageUrl?: string;
}

export interface CategoryStats {
  category: string;
  orderCount: number;
  itemCount: number;
  totalSpent: number;
}

export interface UserProductStats {
  mostPurchasedProducts: ProductPurchaseInfo[];
  topCategories: CategoryStats[];
  totalUniqueProducts: number;
  totalItemsPurchased: number;
}

export interface BestSellingProduct {
  productId: string;
  productName: string;
  totalSold: number;
  revenue: number;
  orderCount: number;
  imageUrl?: string;
}

export interface RecentSale {
  orderId: string;
  productId: string;
  productName: string;
  customerName: string;
  quantity: number;
  amount: number;
  saleDate: Date;
}

export interface SellerProductStats {
  bestSellingProducts: BestSellingProduct[];
  recentSales: RecentSale[];
  totalUniqueProductsSold: number;
  totalCustomers: number;
}
export interface CartItem {
  productId: string;
  name: string;
  productName?: string; // Backend uses productName
  price: number;
  quantity: number;
  imageUrl: string | null;
  stock?: number;
  sellerId: string;
  sellerName: string;
}

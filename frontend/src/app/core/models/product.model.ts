export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  stock: number;
  sellerId: string;
  sellerName: string;
  createdAt?: Date;
  updatedAt?: Date;
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  category: string;
  stock: number;
}

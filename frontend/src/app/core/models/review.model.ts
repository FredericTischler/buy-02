export interface Review {
  id: string;
  productId: string;
  userId: string;
  userName: string;
  rating: number;
  title: string;
  comment: string;
  verified: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewRequest {
  rating: number;
  title?: string;
  comment: string;
}

export interface ProductReviewStats {
  productId: string;
  averageRating: number;
  totalReviews: number;
  ratingDistribution: { [key: number]: number };
}
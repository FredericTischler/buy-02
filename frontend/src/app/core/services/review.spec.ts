import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ReviewService } from './review';
import { Review, ReviewRequest, ProductReviewStats } from '../models/review.model';

describe('ReviewService', () => {
  let service: ReviewService;
  let httpMock: HttpTestingController;

  const sampleReview: Review = {
    id: 'r1',
    productId: 'p1',
    userId: 'u1',
    userName: 'John',
    rating: 5,
    title: 'Great',
    comment: 'Excellent product',
    verified: false,
    createdAt: '2025-01-01T00:00:00',
    updatedAt: '2025-01-01T00:00:00',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ReviewService],
    });

    service = TestBed.inject(ReviewService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create a review', () => {
    const request: ReviewRequest = { rating: 5, comment: 'Great product' };

    service.createReview('p1', request).subscribe(review => {
      expect(review.id).toBe('r1');
      expect(review.rating).toBe(5);
    });

    const req = httpMock.expectOne(r => r.url.includes('/api/reviews/product/p1') && r.method === 'POST');
    expect(req.request.body).toEqual(request);
    req.flush(sampleReview);
  });

  it('should get product reviews', () => {
    service.getProductReviews('p1').subscribe(reviews => {
      expect(reviews.length).toBe(1);
      expect(reviews[0].id).toBe('r1');
    });

    const req = httpMock.expectOne(r => r.url.includes('/api/reviews/product/p1') && r.method === 'GET');
    req.flush([sampleReview]);
  });

  it('should get product stats', () => {
    const stats: ProductReviewStats = {
      productId: 'p1',
      averageRating: 4.5,
      totalReviews: 10,
      ratingDistribution: { 5: 5, 4: 3, 3: 1, 2: 1, 1: 0 },
    };

    service.getProductStats('p1').subscribe(result => {
      expect(result.averageRating).toBe(4.5);
      expect(result.totalReviews).toBe(10);
    });

    const req = httpMock.expectOne(r => r.url.includes('/api/reviews/product/p1/stats'));
    req.flush(stats);
  });

  it('should get my reviews', () => {
    service.getMyReviews().subscribe(reviews => {
      expect(reviews.length).toBe(1);
    });

    const req = httpMock.expectOne(r => r.url.includes('/api/reviews/my-reviews'));
    req.flush([sampleReview]);
  });

  it('should get a single review', () => {
    service.getReview('r1').subscribe(review => {
      expect(review.id).toBe('r1');
    });

    const req = httpMock.expectOne(r => r.url.includes('/api/reviews/r1') && r.method === 'GET');
    req.flush(sampleReview);
  });

  it('should update a review', () => {
    const request: ReviewRequest = { rating: 4, comment: 'Updated' };

    service.updateReview('r1', request).subscribe(review => {
      expect(review.rating).toBe(4);
    });

    const req = httpMock.expectOne(r => r.url.includes('/api/reviews/r1') && r.method === 'PUT');
    req.flush({ ...sampleReview, rating: 4, comment: 'Updated' });
  });

  it('should delete a review', () => {
    service.deleteReview('r1').subscribe(result => {
      expect(result.message).toBe('Review deleted successfully');
    });

    const req = httpMock.expectOne(r => r.url.includes('/api/reviews/r1') && r.method === 'DELETE');
    req.flush({ message: 'Review deleted successfully' });
  });

  it('should check if user can review', () => {
    service.canReview('p1').subscribe(result => {
      expect(result.canReview).toBeTrue();
    });

    const req = httpMock.expectOne(r => r.url.includes('/api/reviews/product/p1/can-review'));
    req.flush({ canReview: true });
  });

  it('should get my review for a product', () => {
    service.getMyReviewForProduct('p1').subscribe(review => {
      expect(review.id).toBe('r1');
    });

    const req = httpMock.expectOne(r => r.url.includes('/api/reviews/product/p1/my-review'));
    req.flush(sampleReview);
  });
});
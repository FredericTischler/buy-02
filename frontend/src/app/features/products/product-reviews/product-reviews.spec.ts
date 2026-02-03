import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ProductReviews } from './product-reviews';
import { ReviewService } from '../../../core/services/review';
import { Auth } from '../../../core/services/auth';
import { Review, ProductReviewStats } from '../../../core/models/review.model';

describe('ProductReviews', () => {
  let component: ProductReviews;
  let fixture: ComponentFixture<ProductReviews>;
  let reviewService: jasmine.SpyObj<ReviewService>;
  let authService: jasmine.SpyObj<Auth>;
  let snackBarSpy: jasmine.Spy;

  const mockReview: Review = {
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

  const mockStats: ProductReviewStats = {
    productId: 'p1',
    averageRating: 4.5,
    totalReviews: 10,
    ratingDistribution: { 5: 5, 4: 3, 3: 1, 2: 1, 1: 0 },
  };

  beforeEach(async () => {
    reviewService = jasmine.createSpyObj<ReviewService>('ReviewService', [
      'getProductReviews',
      'getProductStats',
      'canReview',
      'getMyReviewForProduct',
      'createReview',
      'updateReview',
      'deleteReview',
    ]);
    authService = jasmine.createSpyObj<Auth>('Auth', ['isLoggedIn']);

    reviewService.getProductReviews.and.returnValue(of([mockReview]));
    reviewService.getProductStats.and.returnValue(of(mockStats));
    reviewService.canReview.and.returnValue(of({ canReview: true }));
    authService.isLoggedIn.and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [ProductReviews, HttpClientTestingModule, NoopAnimationsModule],
    })
    .overrideComponent(ProductReviews, {
      set: {
        providers: [
          { provide: ReviewService, useValue: reviewService },
          { provide: Auth, useValue: authService },
        ],
      },
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProductReviews);
    component = fixture.componentInstance;
    component.productId = 'p1';

    // Spy on the component's snackBar instance
    snackBarSpy = spyOn((component as any).snackBar, 'open');
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load reviews on init', () => {
    fixture.detectChanges();
    expect(reviewService.getProductReviews).toHaveBeenCalledWith('p1');
    expect(component.reviews.length).toBe(1);
  });

  it('should load stats after reviews', () => {
    fixture.detectChanges();
    expect(reviewService.getProductStats).toHaveBeenCalledWith('p1');
    expect(component.stats).toEqual(mockStats);
    expect(component.loading).toBeFalse();
  });

  it('should check if user can review', () => {
    fixture.detectChanges();
    expect(reviewService.canReview).toHaveBeenCalledWith('p1');
    expect(component.canReview).toBeTrue();
  });

  it('should not check canReview when not logged in', () => {
    authService.isLoggedIn.and.returnValue(false);
    fixture.detectChanges();
    expect(component.canReview).toBeFalse();
  });

  it('should set rating', () => {
    component.setRating(4);
    expect(component.rating).toBe(4);
  });

  it('should set and clear hover rating', () => {
    component.setHoverRating(3);
    expect(component.hoverRating).toBe(3);
    component.clearHoverRating();
    expect(component.hoverRating).toBe(0);
  });

  it('should return correct star class', () => {
    component.rating = 3;
    component.hoverRating = 0;
    expect(component.getStarClass(3)).toBe('star-filled');
    expect(component.getStarClass(4)).toBe('star-empty');
  });

  it('should show snackbar when submitting without rating', () => {
    component.rating = 0;
    component.comment = 'Some comment';
    component.submitReview();
    expect(snackBarSpy).toHaveBeenCalledWith('Veuillez donner une note', 'Fermer', { duration: 3000 });
  });

  it('should show snackbar when submitting without comment', () => {
    component.rating = 5;
    component.comment = '';
    component.submitReview();
    expect(snackBarSpy).toHaveBeenCalledWith('Veuillez ajouter un commentaire', 'Fermer', { duration: 3000 });
  });

  it('should create review on submit', () => {
    fixture.detectChanges();
    reviewService.createReview.and.returnValue(of(mockReview));

    component.rating = 5;
    component.title = 'Great';
    component.comment = 'Excellent product';
    component.submitReview();

    expect(reviewService.createReview).toHaveBeenCalledWith('p1', {
      rating: 5,
      title: 'Great',
      comment: 'Excellent product',
    });
    expect(snackBarSpy).toHaveBeenCalledWith('Avis ajouté avec succès', 'Fermer', { duration: 3000 });
  });

  it('should update review in edit mode', () => {
    fixture.detectChanges();
    reviewService.updateReview.and.returnValue(of(mockReview));

    component.editMode = true;
    component.userReview = mockReview;
    component.rating = 4;
    component.title = 'Updated';
    component.comment = 'Updated comment';
    component.submitReview();

    expect(reviewService.updateReview).toHaveBeenCalledWith('r1', {
      rating: 4,
      title: 'Updated',
      comment: 'Updated comment',
    });
  });

  it('should populate form on editReview', () => {
    component.userReview = mockReview;
    component.editReview();

    expect(component.editMode).toBeTrue();
    expect(component.rating).toBe(5);
    expect(component.title).toBe('Great');
    expect(component.comment).toBe('Excellent product');
  });

  it('should cancel edit and reset form', () => {
    component.editMode = true;
    component.rating = 3;
    component.title = 'test';
    component.comment = 'test';

    component.cancelEdit();

    expect(component.editMode).toBeFalse();
    expect(component.rating).toBe(0);
    expect(component.title).toBe('');
    expect(component.comment).toBe('');
  });

  it('should calculate rating percentage', () => {
    component.stats = mockStats;
    expect(component.getRatingPercentage(5)).toBe(50); // 5/10 * 100
    expect(component.getRatingPercentage(1)).toBe(0);
  });

  it('should return 0 percentage when no stats', () => {
    component.stats = null;
    expect(component.getRatingPercentage(5)).toBe(0);
  });

  it('should return star array', () => {
    expect(component.getStarArray()).toEqual([5, 4, 3, 2, 1]);
  });

  it('should format date', () => {
    const formatted = component.formatDate('2025-01-15T10:00:00');
    expect(formatted).toContain('2025');
  });

  it('should delegate isLoggedIn to authService', () => {
    authService.isLoggedIn.and.returnValue(true);
    expect(component.isLoggedIn()).toBeTrue();

    authService.isLoggedIn.and.returnValue(false);
    expect(component.isLoggedIn()).toBeFalse();
  });

  it('should handle create review error', () => {
    fixture.detectChanges();
    reviewService.createReview.and.returnValue(throwError(() => ({ error: { message: 'Already reviewed' } })));

    component.rating = 5;
    component.comment = 'Test';
    component.submitReview();

    expect(snackBarSpy).toHaveBeenCalledWith('Already reviewed', 'Fermer', { duration: 3000 });
    expect(component.submitting).toBeFalse();
  });

  it('should handle reviews loading error', () => {
    reviewService.getProductReviews.and.returnValue(throwError(() => new Error('Network error')));
    fixture.detectChanges();

    expect(component.loading).toBeFalse();
  });
});
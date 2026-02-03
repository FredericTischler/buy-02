import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { Review, ReviewRequest, ProductReviewStats } from '../../../core/models/review.model';
import { ReviewService } from '../../../core/services/review';
import { Auth } from '../../../core/services/auth';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-product-reviews',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule
  ],
  templateUrl: './product-reviews.html',
  styleUrl: './product-reviews.scss'
})
export class ProductReviews implements OnInit {
  @Input() productId!: string;

  reviews: Review[] = [];
  stats: ProductReviewStats | null = null;
  loading = true;
  submitting = false;
  canReview = false;
  userReview: Review | null = null;
  editMode = false;

  // Form fields
  rating = 0;
  hoverRating = 0;
  title = '';
  comment = '';

  constructor(
    private readonly reviewService: ReviewService,
    private readonly authService: Auth,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    if (this.productId) {
      this.loadReviews();
      this.checkCanReview();
    }
  }

  loadReviews(): void {
    this.loading = true;

    this.reviewService.getProductReviews(this.productId).subscribe({
      next: (reviews) => {
        this.reviews = reviews;
        this.loadStats();
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadStats(): void {
    this.reviewService.getProductStats(this.productId).subscribe({
      next: (stats) => {
        this.stats = stats;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  checkCanReview(): void {
    if (!this.authService.isLoggedIn()) {
      this.canReview = false;
      return;
    }

    this.reviewService.canReview(this.productId).pipe(
      catchError(() => of({ canReview: false }))
    ).subscribe({
      next: (result) => {
        this.canReview = result.canReview;
        if (!result.canReview) {
          this.loadUserReview();
        }
      }
    });
  }

  loadUserReview(): void {
    this.reviewService.getMyReviewForProduct(this.productId).pipe(
      catchError(() => of(null))
    ).subscribe({
      next: (review) => {
        if (review) {
          this.userReview = review;
        }
      }
    });
  }

  setRating(value: number): void {
    this.rating = value;
  }

  setHoverRating(value: number): void {
    this.hoverRating = value;
  }

  clearHoverRating(): void {
    this.hoverRating = 0;
  }

  getStarClass(index: number): string {
    const displayRating = this.hoverRating || this.rating;
    return index <= displayRating ? 'star-filled' : 'star-empty';
  }

  submitReview(): void {
    if (this.rating === 0) {
      this.snackBar.open('Veuillez donner une note', 'Fermer', { duration: 3000 });
      return;
    }

    if (!this.comment.trim()) {
      this.snackBar.open('Veuillez ajouter un commentaire', 'Fermer', { duration: 3000 });
      return;
    }

    this.submitting = true;
    const request: ReviewRequest = {
      rating: this.rating,
      title: this.title.trim() || undefined,
      comment: this.comment.trim()
    };

    if (this.editMode && this.userReview) {
      this.reviewService.updateReview(this.userReview.id, request).subscribe({
        next: () => {
          this.snackBar.open('Avis mis à jour', 'Fermer', { duration: 3000 });
          this.resetForm();
          this.loadReviews();
          this.checkCanReview();
          this.submitting = false;
          this.editMode = false;
        },
        error: (error) => {
          this.snackBar.open(error.error?.message || 'Erreur lors de la mise à jour', 'Fermer', { duration: 3000 });
          this.submitting = false;
        }
      });
    } else {
      this.reviewService.createReview(this.productId, request).subscribe({
        next: () => {
          this.snackBar.open('Avis ajouté avec succès', 'Fermer', { duration: 3000 });
          this.resetForm();
          this.loadReviews();
          this.checkCanReview();
          this.submitting = false;
        },
        error: (error) => {
          this.snackBar.open(error.error?.message || 'Erreur lors de l\'ajout de l\'avis', 'Fermer', { duration: 3000 });
          this.submitting = false;
        }
      });
    }
  }

  editReview(): void {
    if (this.userReview) {
      this.editMode = true;
      this.rating = this.userReview.rating;
      this.title = this.userReview.title || '';
      this.comment = this.userReview.comment;
    }
  }

  deleteReview(): void {
    if (!this.userReview) return;

    if (confirm('Êtes-vous sûr de vouloir supprimer votre avis ?')) {
      this.reviewService.deleteReview(this.userReview.id).subscribe({
        next: () => {
          this.snackBar.open('Avis supprimé', 'Fermer', { duration: 3000 });
          this.userReview = null;
          this.canReview = true;
          this.loadReviews();
        },
        error: () => {
          this.snackBar.open('Erreur lors de la suppression', 'Fermer', { duration: 3000 });
        }
      });
    }
  }

  cancelEdit(): void {
    this.editMode = false;
    this.resetForm();
  }

  resetForm(): void {
    this.rating = 0;
    this.hoverRating = 0;
    this.title = '';
    this.comment = '';
  }

  getRatingPercentage(star: number): number {
    if (!this.stats || this.stats.totalReviews === 0) return 0;
    const count = this.stats.ratingDistribution[star] || 0;
    return (count / this.stats.totalReviews) * 100;
  }

  getStarArray(): number[] {
    return [5, 4, 3, 2, 1];
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }
}
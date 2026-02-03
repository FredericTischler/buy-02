package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductReviewStats;
import com.ecommerce.product.dto.ReviewRequest;
import com.ecommerce.product.dto.ReviewResponse;
import com.ecommerce.product.model.Review;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    private ReviewService reviewService;

    private static final String USER_ID = "user-123";
    private static final String USER_NAME = "John Doe";
    private static final String PRODUCT_ID = "product-1";

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, productRepository);
    }

    // ==================== CREATE REVIEW TESTS ====================

    @Test
    void createReview_shouldCreateSuccessfully() {
        ReviewRequest request = new ReviewRequest(5, "Great", "Excellent product");

        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId("review-1");
            return review;
        });

        ReviewResponse response = reviewService.createReview(PRODUCT_ID, request, USER_ID, USER_NAME);

        assertThat(response.getId()).isEqualTo("review-1");
        assertThat(response.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Excellent product");
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_shouldThrowWhenProductNotFound() {
        ReviewRequest request = new ReviewRequest(5, "Great", "Excellent product");
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview(PRODUCT_ID, request, USER_ID, USER_NAME))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Product not found");
    }

    @Test
    void createReview_shouldThrowWhenAlreadyReviewed() {
        ReviewRequest request = new ReviewRequest(5, "Great", "Excellent product");
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(PRODUCT_ID, request, USER_ID, USER_NAME))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("You have already reviewed this product");
    }

    // ==================== GET REVIEWS TESTS ====================

    @Test
    void getReviewsByProduct_shouldReturnReviews() {
        List<Review> reviews = List.of(createReview("r1", 5), createReview("r2", 4));
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(PRODUCT_ID)).thenReturn(reviews);

        List<ReviewResponse> responses = reviewService.getReviewsByProduct(PRODUCT_ID);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo("r1");
        assertThat(responses.get(1).getId()).isEqualTo("r2");
    }

    @Test
    void getReviewsByUser_shouldReturnUserReviews() {
        List<Review> reviews = List.of(createReview("r1", 5));
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(reviews);

        List<ReviewResponse> responses = reviewService.getReviewsByUser(USER_ID);

        assertThat(responses).hasSize(1);
    }

    @Test
    void getReviewById_shouldReturnReview() {
        Review review = createReview("r1", 5);
        when(reviewRepository.findById("r1")).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.getReviewById("r1");

        assertThat(response.getId()).isEqualTo("r1");
        assertThat(response.getRating()).isEqualTo(5);
    }

    @Test
    void getReviewById_shouldThrowWhenNotFound() {
        when(reviewRepository.findById("r1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewById("r1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Review not found");
    }

    // ==================== UPDATE REVIEW TESTS ====================

    @Test
    void updateReview_shouldUpdateSuccessfully() {
        Review review = createReview("r1", 3);
        ReviewRequest request = new ReviewRequest(5, "Updated", "Much better now");

        when(reviewRepository.findById("r1")).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.updateReview("r1", request, USER_ID);

        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getTitle()).isEqualTo("Updated");
        assertThat(response.getComment()).isEqualTo("Much better now");
    }

    @Test
    void updateReview_shouldThrowWhenNotOwner() {
        Review review = createReview("r1", 3);
        ReviewRequest request = new ReviewRequest(5, "Updated", "Better");

        when(reviewRepository.findById("r1")).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview("r1", request, "other-user"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("You are not authorized to update this review");
    }

    @Test
    void updateReview_shouldThrowWhenNotFound() {
        ReviewRequest request = new ReviewRequest(5, "Updated", "Better");
        when(reviewRepository.findById("r1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview("r1", request, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Review not found");
    }

    // ==================== DELETE REVIEW TESTS ====================

    @Test
    void deleteReview_shouldDeleteSuccessfully() {
        Review review = createReview("r1", 5);
        when(reviewRepository.findById("r1")).thenReturn(Optional.of(review));

        reviewService.deleteReview("r1", USER_ID);

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_shouldThrowWhenNotOwner() {
        Review review = createReview("r1", 5);
        when(reviewRepository.findById("r1")).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview("r1", "other-user"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("You are not authorized to delete this review");
    }

    @Test
    void deleteReview_shouldThrowWhenNotFound() {
        when(reviewRepository.findById("r1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview("r1", USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Review not found");
    }

    // ==================== GET PRODUCT STATS TESTS ====================

    @Test
    void getProductStats_shouldCalculateStatsCorrectly() {
        List<Review> reviews = List.of(
                createReview("r1", 5),
                createReview("r2", 4),
                createReview("r3", 5),
                createReview("r4", 3)
        );
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(PRODUCT_ID)).thenReturn(reviews);

        ProductReviewStats stats = reviewService.getProductStats(PRODUCT_ID);

        assertThat(stats.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(stats.getTotalReviews()).isEqualTo(4);
        assertThat(stats.getAverageRating()).isEqualTo(4.3); // (5+4+5+3)/4 = 4.25 rounded to 4.3
        assertThat(stats.getRatingDistribution()).containsEntry(5, 2L);
        assertThat(stats.getRatingDistribution()).containsEntry(4, 1L);
        assertThat(stats.getRatingDistribution()).containsEntry(3, 1L);
        assertThat(stats.getRatingDistribution()).containsEntry(2, 0L);
        assertThat(stats.getRatingDistribution()).containsEntry(1, 0L);
    }

    @Test
    void getProductStats_shouldReturnEmptyStatsWhenNoReviews() {
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(PRODUCT_ID)).thenReturn(List.of());

        ProductReviewStats stats = reviewService.getProductStats(PRODUCT_ID);

        assertThat(stats.getTotalReviews()).isEqualTo(0);
        assertThat(stats.getAverageRating()).isEqualTo(0.0);
        assertThat(stats.getRatingDistribution()).hasSize(5);
    }

    // ==================== CAN USER REVIEW TESTS ====================

    @Test
    void canUserReview_shouldReturnTrueWhenNotReviewed() {
        when(reviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);

        boolean result = reviewService.canUserReview(PRODUCT_ID, USER_ID);

        assertThat(result).isTrue();
    }

    @Test
    void canUserReview_shouldReturnFalseWhenAlreadyReviewed() {
        when(reviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(true);

        boolean result = reviewService.canUserReview(PRODUCT_ID, USER_ID);

        assertThat(result).isFalse();
    }

    // ==================== GET USER REVIEW FOR PRODUCT TESTS ====================

    @Test
    void getUserReviewForProduct_shouldReturnReviewIfExists() {
        Review review = createReview("r1", 5);
        when(reviewRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.getUserReviewForProduct(PRODUCT_ID, USER_ID);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("r1");
    }

    @Test
    void getUserReviewForProduct_shouldReturnNullIfNotExists() {
        when(reviewRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.empty());

        ReviewResponse response = reviewService.getUserReviewForProduct(PRODUCT_ID, USER_ID);

        assertThat(response).isNull();
    }

    // ==================== HELPER METHODS ====================

    private Review createReview(String id, int rating) {
        Review review = new Review();
        review.setId(id);
        review.setProductId(PRODUCT_ID);
        review.setUserId(USER_ID);
        review.setUserName(USER_NAME);
        review.setRating(rating);
        review.setTitle("Test Title");
        review.setComment("Test comment");
        review.setVerified(false);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        return review;
    }
}
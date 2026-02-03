package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductReviewStats;
import com.ecommerce.product.dto.ReviewRequest;
import com.ecommerce.product.dto.ReviewResponse;
import com.ecommerce.product.model.Review;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    /**
     * Create a new review for a product
     */
    @Transactional
    public ReviewResponse createReview(String productId, ReviewRequest request, String userId, String userName) {
        log.info("Creating review for product {} by user {}", productId, userId);

        // Verify product exists
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Product not found");
        }

        // Check if user already reviewed this product
        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new RuntimeException("You have already reviewed this product");
        }

        Review review = new Review();
        review.setProductId(productId);
        review.setUserId(userId);
        review.setUserName(userName);
        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());
        review.setVerified(false); // Could be set to true if we verify purchase
        review.prePersist();

        Review savedReview = reviewRepository.save(review);
        log.info("Review created with ID: {}", savedReview.getId());

        return ReviewResponse.fromReview(savedReview);
    }

    /**
     * Get all reviews for a product
     */
    public List<ReviewResponse> getReviewsByProduct(String productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(ReviewResponse::fromReview)
                .collect(Collectors.toList());
    }

    /**
     * Get all reviews by a user
     */
    public List<ReviewResponse> getReviewsByUser(String userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ReviewResponse::fromReview)
                .collect(Collectors.toList());
    }

    /**
     * Get review by ID
     */
    public ReviewResponse getReviewById(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        return ReviewResponse.fromReview(review);
    }

    /**
     * Update a review
     */
    @Transactional
    public ReviewResponse updateReview(String reviewId, ReviewRequest request, String userId) {
        log.info("Updating review {} by user {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!review.getUserId().equals(userId)) {
            throw new RuntimeException("You are not authorized to update this review");
        }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);
        log.info("Review {} updated", reviewId);

        return ReviewResponse.fromReview(savedReview);
    }

    /**
     * Delete a review
     */
    @Transactional
    public void deleteReview(String reviewId, String userId) {
        log.info("Deleting review {} by user {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!review.getUserId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this review");
        }

        reviewRepository.delete(review);
        log.info("Review {} deleted", reviewId);
    }

    /**
     * Get product review statistics
     */
    public ProductReviewStats getProductStats(String productId) {
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);

        if (reviews.isEmpty()) {
            Map<Integer, Long> emptyDistribution = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                emptyDistribution.put(i, 0L);
            }
            return new ProductReviewStats(productId, 0.0, 0L, emptyDistribution);
        }

        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        Map<Integer, Long> ratingDistribution = reviews.stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        // Ensure all ratings 1-5 are present
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.putIfAbsent(i, 0L);
        }

        return new ProductReviewStats(
                productId,
                Math.round(averageRating * 10.0) / 10.0, // Round to 1 decimal
                (long) reviews.size(),
                ratingDistribution
        );
    }

    /**
     * Check if user can review a product (hasn't already reviewed)
     */
    public boolean canUserReview(String productId, String userId) {
        return !reviewRepository.existsByUserIdAndProductId(userId, productId);
    }

    /**
     * Get user's review for a product (if exists)
     */
    public ReviewResponse getUserReviewForProduct(String productId, String userId) {
        return reviewRepository.findByUserIdAndProductId(userId, productId)
                .map(ReviewResponse::fromReview)
                .orElse(null);
    }
}
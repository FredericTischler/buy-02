package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductReviewStats;
import com.ecommerce.product.dto.ReviewRequest;
import com.ecommerce.product.dto.ReviewResponse;
import com.ecommerce.product.security.JwtUtil;
import com.ecommerce.product.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtUtil jwtUtil;

    /**
     * Create a new review for a product
     */
    @PostMapping("/product/{productId}")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable String productId,
            @Valid @RequestBody ReviewRequest request,
            @RequestHeader("Authorization") String token) {
        String userId = extractUserId(token);
        String userName = extractUserName(token);
        log.info("Creating review for product {} by user {}", productId, userId);
        ReviewResponse response = reviewService.createReview(productId, request, userId, userName);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all reviews for a product
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewResponse>> getProductReviews(@PathVariable String productId) {
        log.info("Getting reviews for product {}", productId);
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    /**
     * Get review statistics for a product
     */
    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<ProductReviewStats> getProductStats(@PathVariable String productId) {
        log.info("Getting review stats for product {}", productId);
        return ResponseEntity.ok(reviewService.getProductStats(productId));
    }

    /**
     * Get all reviews by the authenticated user
     */
    @GetMapping("/my-reviews")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(@RequestHeader("Authorization") String token) {
        String userId = extractUserId(token);
        log.info("Getting reviews for user {}", userId);
        return ResponseEntity.ok(reviewService.getReviewsByUser(userId));
    }

    /**
     * Get a specific review by ID
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable String reviewId) {
        log.info("Getting review {}", reviewId);
        return ResponseEntity.ok(reviewService.getReviewById(reviewId));
    }

    /**
     * Update a review
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewRequest request,
            @RequestHeader("Authorization") String token) {
        String userId = extractUserId(token);
        log.info("Updating review {} by user {}", reviewId, userId);
        return ResponseEntity.ok(reviewService.updateReview(reviewId, request, userId));
    }

    /**
     * Delete a review
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteReview(
            @PathVariable String reviewId,
            @RequestHeader("Authorization") String token) {
        String userId = extractUserId(token);
        log.info("Deleting review {} by user {}", reviewId, userId);
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
    }

    /**
     * Check if user can review a product
     */
    @GetMapping("/product/{productId}/can-review")
    public ResponseEntity<Map<String, Boolean>> canReview(
            @PathVariable String productId,
            @RequestHeader("Authorization") String token) {
        String userId = extractUserId(token);
        boolean canReview = reviewService.canUserReview(productId, userId);
        return ResponseEntity.ok(Map.of("canReview", canReview));
    }

    /**
     * Get user's review for a product (if exists)
     */
    @GetMapping("/product/{productId}/my-review")
    public ResponseEntity<ReviewResponse> getMyReviewForProduct(
            @PathVariable String productId,
            @RequestHeader("Authorization") String token) {
        String userId = extractUserId(token);
        ReviewResponse review = reviewService.getUserReviewForProduct(productId, userId);
        if (review == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(review);
    }

    private String extractUserId(String token) {
        String jwt = token.replace("Bearer ", "");
        return jwtUtil.extractUserId(jwt);
    }

    private String extractUserName(String token) {
        String jwt = token.replace("Bearer ", "");
        return jwtUtil.extractName(jwt);
    }
}
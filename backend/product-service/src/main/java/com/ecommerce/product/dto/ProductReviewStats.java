package com.ecommerce.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewStats {

    private String productId;
    private double averageRating;
    private long totalReviews;
    private Map<Integer, Long> ratingDistribution; // e.g., {5: 10, 4: 5, 3: 2, 2: 1, 1: 0}
}
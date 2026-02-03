package com.ecommerce.product.repository;

import com.ecommerce.product.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByProductIdOrderByCreatedAtDesc(String productId);

    List<Review> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Review> findByUserIdAndProductId(String userId, String productId);

    boolean existsByUserIdAndProductId(String userId, String productId);

    long countByProductId(String productId);

    @Query(value = "{ 'productId': ?0 }", fields = "{ 'rating': 1 }")
    List<Review> findRatingsByProductId(String productId);

    void deleteByProductId(String productId);
}
package com.ecommerce.order.repository;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    // Find orders by user
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    // Find orders by user and status
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, OrderStatus status);

    // Find orders for a seller (orders containing their products)
    @Query("{ 'items.sellerId': ?0 }")
    List<Order> findBySellerId(String sellerId);

    // Find orders for a seller with specific status
    @Query("{ 'items.sellerId': ?0, 'status': ?1 }")
    List<Order> findBySellerIdAndStatus(String sellerId, OrderStatus status);

    // Find orders by status
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    // Find orders created between dates
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Find orders by user created between dates (for statistics)
    List<Order> findByUserIdAndCreatedAtBetween(String userId, LocalDateTime start, LocalDateTime end);

    // Count orders by user
    long countByUserId(String userId);

    // Count orders by status
    long countByStatus(OrderStatus status);

    // Count orders by user and status
    long countByUserIdAndStatus(String userId, OrderStatus status);

    // Search orders by user name or email (for admin)
    @Query("{ $or: [ { 'userName': { $regex: ?0, $options: 'i' } }, { 'userEmail': { $regex: ?0, $options: 'i' } } ] }")
    List<Order> searchByUserNameOrEmail(String keyword);
}
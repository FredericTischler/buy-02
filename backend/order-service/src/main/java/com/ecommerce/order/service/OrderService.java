package com.ecommerce.order.service;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    private static final String ORDER_EVENTS_TOPIC = "order-events";

    /**
     * Create a new order from checkout
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request, String userId, String userName, String userEmail) {
        log.info("Creating order for user: {}", userId);

        Order order = new Order();
        order.setUserId(userId);
        order.setUserName(userName);
        order.setUserEmail(userEmail);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCity(request.getShippingCity());
        order.setShippingPostalCode(request.getShippingPostalCode());
        order.setShippingCountry(request.getShippingCountry());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "COD");
        order.setNotes(request.getNotes());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Convert request items to order items
        List<OrderItem> orderItems = request.getItems().stream()
                .map(this::toOrderItem)
                .collect(Collectors.toList());
        order.setItems(orderItems);

        // Calculate total
        order.calculateTotalAmount();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());

        // Publish order created event (to decrement stock)
        publishOrderEvent(savedOrder, OrderEvent.EventType.ORDER_CREATED);

        return OrderResponse.fromOrder(savedOrder);
    }

    /**
     * Get order by ID
     */
    public OrderResponse getOrderById(String orderId, String userId, boolean isSeller) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Check access: user can see their own orders, seller can see orders with their products
        if (!order.getUserId().equals(userId)) {
            if (isSeller) {
                boolean hasSellerProducts = order.getItems().stream()
                        .anyMatch(item -> item.getSellerId().equals(userId));
                if (!hasSellerProducts) {
                    throw new RuntimeException("You are not authorized to view this order");
                }
            } else {
                throw new RuntimeException("You are not authorized to view this order");
            }
        }

        return OrderResponse.fromOrder(order);
    }

    /**
     * Get all orders for a user (customer)
     */
    public List<OrderResponse> getOrdersByUser(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(OrderResponse::fromOrder)
                .collect(Collectors.toList());
    }

    /**
     * Get orders for a user with specific status
     */
    public List<OrderResponse> getOrdersByUserAndStatus(String userId, OrderStatus status) {
        return orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status)
                .stream()
                .map(OrderResponse::fromOrder)
                .collect(Collectors.toList());
    }

    /**
     * Get orders for a seller (orders containing their products)
     */
    public List<OrderResponse> getOrdersForSeller(String sellerId) {
        return orderRepository.findBySellerId(sellerId)
                .stream()
                .map(order -> filterOrderItemsForSeller(order, sellerId))
                .map(OrderResponse::fromOrder)
                .collect(Collectors.toList());
    }

    /**
     * Get orders for a seller with specific status
     */
    public List<OrderResponse> getOrdersForSellerByStatus(String sellerId, OrderStatus status) {
        return orderRepository.findBySellerIdAndStatus(sellerId, status)
                .stream()
                .map(order -> filterOrderItemsForSeller(order, sellerId))
                .map(OrderResponse::fromOrder)
                .collect(Collectors.toList());
    }

    /**
     * Update order status (for sellers)
     */
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, StatusUpdateRequest request, String sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verify seller has products in this order
        boolean hasSellerProducts = order.getItems().stream()
                .anyMatch(item -> item.getSellerId().equals(sellerId));
        if (!hasSellerProducts) {
            throw new RuntimeException("You are not authorized to update this order");
        }

        // Validate status transition
        validateStatusTransition(order.getStatus(), request.getStatus());

        OrderStatus newStatus = request.getStatus();
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        // Set timestamp based on status
        switch (newStatus) {
            case CONFIRMED -> order.setConfirmedAt(LocalDateTime.now());
            case SHIPPED -> order.setShippedAt(LocalDateTime.now());
            case DELIVERED -> order.setDeliveredAt(LocalDateTime.now());
            case CANCELLED -> {
                order.setCancelledAt(LocalDateTime.now());
                order.setCancellationReason(request.getReason());
            }
            default -> { /* No special timestamp */ }
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated to {}", orderId, newStatus);

        // Publish event based on status
        OrderEvent.EventType eventType = switch (newStatus) {
            case CONFIRMED -> OrderEvent.EventType.ORDER_CONFIRMED;
            case SHIPPED -> OrderEvent.EventType.ORDER_SHIPPED;
            case DELIVERED -> OrderEvent.EventType.ORDER_DELIVERED;
            case CANCELLED -> OrderEvent.EventType.ORDER_CANCELLED;
            default -> null;
        };

        if (eventType != null) {
            publishOrderEvent(savedOrder, eventType);
        }

        return OrderResponse.fromOrder(savedOrder);
    }

    /**
     * Cancel order (for customers - only if PENDING)
     */
    @Transactional
    public OrderResponse cancelOrder(String orderId, String userId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("You are not authorized to cancel this order");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException("Order cannot be cancelled in current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason != null ? reason : "Cancelled by customer");
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} cancelled by user {}", orderId, userId);

        // Publish cancellation event (to restore stock)
        publishOrderEvent(savedOrder, OrderEvent.EventType.ORDER_CANCELLED);

        return OrderResponse.fromOrder(savedOrder);
    }

    /**
     * Reorder - create a new order from an existing one
     */
    @Transactional
    public OrderResponse reorder(String orderId, String userId, String userName, String userEmail) {
        Order originalOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!originalOrder.getUserId().equals(userId)) {
            throw new RuntimeException("You are not authorized to reorder this order");
        }

        // Create new order with same items and shipping info
        OrderRequest request = new OrderRequest();
        request.setItems(originalOrder.getItems().stream()
                .map(item -> {
                    OrderItemRequest itemRequest = new OrderItemRequest();
                    itemRequest.setProductId(item.getProductId());
                    itemRequest.setProductName(item.getProductName());
                    itemRequest.setSellerId(item.getSellerId());
                    itemRequest.setSellerName(item.getSellerName());
                    itemRequest.setPrice(item.getPrice());
                    itemRequest.setQuantity(item.getQuantity());
                    itemRequest.setImageUrl(item.getImageUrl());
                    return itemRequest;
                })
                .collect(Collectors.toList()));
        request.setShippingAddress(originalOrder.getShippingAddress());
        request.setShippingCity(originalOrder.getShippingCity());
        request.setShippingPostalCode(originalOrder.getShippingPostalCode());
        request.setShippingCountry(originalOrder.getShippingCountry());
        request.setPhoneNumber(originalOrder.getPhoneNumber());
        request.setPaymentMethod(originalOrder.getPaymentMethod());

        return createOrder(request, userId, userName, userEmail);
    }

    /**
     * Get user statistics
     */
    public UserOrderStats getUserStats(String userId) {
        List<Order> userOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);

        long totalOrders = userOrders.size();
        long completedOrders = userOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .count();
        double totalSpent = userOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .mapToDouble(Order::getTotalAmount)
                .sum();

        return new UserOrderStats(totalOrders, completedOrders, totalSpent);
    }

    /**
     * Get seller statistics
     */
    public SellerOrderStats getSellerStats(String sellerId) {
        List<Order> sellerOrders = orderRepository.findBySellerId(sellerId);

        long totalOrders = sellerOrders.size();
        long completedOrders = sellerOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .count();

        double totalRevenue = sellerOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .flatMap(o -> o.getItems().stream())
                .filter(item -> item.getSellerId().equals(sellerId))
                .mapToDouble(OrderItem::getSubtotal)
                .sum();

        long totalItemsSold = sellerOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .flatMap(o -> o.getItems().stream())
                .filter(item -> item.getSellerId().equals(sellerId))
                .mapToLong(OrderItem::getQuantity)
                .sum();

        return new SellerOrderStats(totalOrders, completedOrders, totalRevenue, totalItemsSold);
    }

    // Helper methods

    private OrderItem toOrderItem(OrderItemRequest request) {
        return new OrderItem(
                request.getProductId(),
                request.getProductName(),
                request.getSellerId(),
                request.getSellerName(),
                request.getPrice(),
                request.getQuantity(),
                request.getImageUrl()
        );
    }

    private Order filterOrderItemsForSeller(Order order, String sellerId) {
        // Create a copy with only this seller's items (for privacy)
        Order filtered = new Order();
        filtered.setId(order.getId());
        filtered.setUserId(order.getUserId());
        filtered.setUserName(order.getUserName());
        filtered.setUserEmail(order.getUserEmail());
        filtered.setStatus(order.getStatus());
        filtered.setShippingAddress(order.getShippingAddress());
        filtered.setShippingCity(order.getShippingCity());
        filtered.setShippingPostalCode(order.getShippingPostalCode());
        filtered.setShippingCountry(order.getShippingCountry());
        filtered.setPhoneNumber(order.getPhoneNumber());
        filtered.setPaymentMethod(order.getPaymentMethod());
        filtered.setNotes(order.getNotes());
        filtered.setCreatedAt(order.getCreatedAt());
        filtered.setUpdatedAt(order.getUpdatedAt());
        filtered.setConfirmedAt(order.getConfirmedAt());
        filtered.setShippedAt(order.getShippedAt());
        filtered.setDeliveredAt(order.getDeliveredAt());
        filtered.setCancelledAt(order.getCancelledAt());
        filtered.setCancellationReason(order.getCancellationReason());

        // Filter items to only show seller's products
        List<OrderItem> sellerItems = order.getItems().stream()
                .filter(item -> item.getSellerId().equals(sellerId))
                .collect(Collectors.toList());
        filtered.setItems(sellerItems);

        // Recalculate total for seller's items only
        filtered.setTotalAmount(sellerItems.stream()
                .mapToDouble(OrderItem::getSubtotal)
                .sum());

        return filtered;
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PROCESSING || next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case PROCESSING -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED -> next == OrderStatus.REFUNDED;
            case CANCELLED, REFUNDED -> false;
        };

        if (!valid) {
            throw new RuntimeException("Invalid status transition from " + current + " to " + next);
        }
    }

    private void publishOrderEvent(Order order, OrderEvent.EventType eventType) {
        try {
            OrderEvent event = new OrderEvent();
            event.setType(eventType);
            event.setOrderId(order.getId());
            event.setUserId(order.getUserId());
            event.setItems(order.getItems());
            event.setStatus(order.getStatus());
            event.setCancellationReason(order.getCancellationReason());

            kafkaTemplate.send(ORDER_EVENTS_TOPIC, order.getId(), event);
            log.info("Published {} event for order {}", eventType, order.getId());
        } catch (Exception e) {
            log.error("Failed to publish order event: {}", e.getMessage());
        }
    }

    /**
     * Get detailed user product statistics (most purchased products, top categories)
     */
    public UserProductStats getUserProductStats(String userId) {
        List<Order> deliveredOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .collect(Collectors.toList());

        // Aggregate product purchases
        java.util.Map<String, UserProductStats.ProductPurchaseInfo> productMap = new java.util.HashMap<>();

        for (Order order : deliveredOrders) {
            for (OrderItem item : order.getItems()) {
                String productId = item.getProductId();
                UserProductStats.ProductPurchaseInfo existing = productMap.get(productId);

                if (existing == null) {
                    existing = new UserProductStats.ProductPurchaseInfo(
                            productId,
                            item.getProductName(),
                            item.getSellerId(),
                            item.getSellerName(),
                            item.getQuantity(),
                            item.getSubtotal(),
                            order.getDeliveredAt() != null ? order.getDeliveredAt() : order.getCreatedAt(),
                            item.getImageUrl()
                    );
                } else {
                    existing.setTotalQuantity(existing.getTotalQuantity() + item.getQuantity());
                    existing.setTotalSpent(existing.getTotalSpent() + item.getSubtotal());
                    LocalDateTime orderDate = order.getDeliveredAt() != null ? order.getDeliveredAt() : order.getCreatedAt();
                    if (orderDate.isAfter(existing.getLastPurchased())) {
                        existing.setLastPurchased(orderDate);
                    }
                }
                productMap.put(productId, existing);
            }
        }

        // Sort by total quantity (most purchased first) and limit to top 10
        List<UserProductStats.ProductPurchaseInfo> mostPurchased = productMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalQuantity(), a.getTotalQuantity()))
                .limit(10)
                .collect(Collectors.toList());

        // Calculate category statistics (using seller name as proxy for category since we don't have category in OrderItem)
        java.util.Map<String, UserProductStats.CategoryStats> categoryMap = new java.util.HashMap<>();

        for (Order order : deliveredOrders) {
            java.util.Set<String> orderSellers = new java.util.HashSet<>();
            for (OrderItem item : order.getItems()) {
                String sellerName = item.getSellerName() != null ? item.getSellerName() : "Unknown";
                orderSellers.add(sellerName);

                UserProductStats.CategoryStats catStats = categoryMap.getOrDefault(sellerName,
                        new UserProductStats.CategoryStats(sellerName, 0, 0, 0.0));
                catStats.setItemCount(catStats.getItemCount() + item.getQuantity());
                catStats.setTotalSpent(catStats.getTotalSpent() + item.getSubtotal());
                categoryMap.put(sellerName, catStats);
            }
            // Increment order count for each seller in this order
            for (String seller : orderSellers) {
                UserProductStats.CategoryStats catStats = categoryMap.get(seller);
                catStats.setOrderCount(catStats.getOrderCount() + 1);
            }
        }

        List<UserProductStats.CategoryStats> topCategories = categoryMap.values().stream()
                .sorted((a, b) -> Double.compare(b.getTotalSpent(), a.getTotalSpent()))
                .limit(5)
                .collect(Collectors.toList());

        int totalUniqueProducts = productMap.size();
        int totalItemsPurchased = productMap.values().stream()
                .mapToInt(UserProductStats.ProductPurchaseInfo::getTotalQuantity)
                .sum();

        return new UserProductStats(mostPurchased, topCategories, totalUniqueProducts, totalItemsPurchased);
    }

    /**
     * Get detailed seller product statistics (best selling products, recent sales)
     */
    public SellerProductStats getSellerProductStats(String sellerId) {
        List<Order> sellerOrders = orderRepository.findBySellerId(sellerId);

        List<Order> deliveredOrders = sellerOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .collect(Collectors.toList());

        // Aggregate best selling products
        java.util.Map<String, SellerProductStats.BestSellingProduct> productMap = new java.util.HashMap<>();
        java.util.Set<String> uniqueCustomers = new java.util.HashSet<>();

        for (Order order : deliveredOrders) {
            uniqueCustomers.add(order.getUserId());
            java.util.Set<String> orderProducts = new java.util.HashSet<>();

            for (OrderItem item : order.getItems()) {
                if (!item.getSellerId().equals(sellerId)) continue;

                String productId = item.getProductId();
                orderProducts.add(productId);

                SellerProductStats.BestSellingProduct existing = productMap.get(productId);

                if (existing == null) {
                    existing = new SellerProductStats.BestSellingProduct(
                            productId,
                            item.getProductName(),
                            item.getQuantity(),
                            item.getSubtotal(),
                            0, // Will increment order count below
                            item.getImageUrl()
                    );
                } else {
                    existing.setTotalSold(existing.getTotalSold() + item.getQuantity());
                    existing.setRevenue(existing.getRevenue() + item.getSubtotal());
                }
                productMap.put(productId, existing);
            }

            // Increment order count for products in this order
            for (String productId : orderProducts) {
                SellerProductStats.BestSellingProduct product = productMap.get(productId);
                if (product != null) {
                    product.setOrderCount(product.getOrderCount() + 1);
                }
            }
        }

        // Sort by total sold (best selling first) and limit to top 10
        List<SellerProductStats.BestSellingProduct> bestSelling = productMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalSold(), a.getTotalSold()))
                .limit(10)
                .collect(Collectors.toList());

        // Get recent sales (last 10 sales across all orders)
        List<SellerProductStats.RecentSale> recentSales = sellerOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.SHIPPED)
                .sorted((a, b) -> {
                    LocalDateTime dateA = a.getDeliveredAt() != null ? a.getDeliveredAt() : a.getCreatedAt();
                    LocalDateTime dateB = b.getDeliveredAt() != null ? b.getDeliveredAt() : b.getCreatedAt();
                    return dateB.compareTo(dateA);
                })
                .flatMap(order -> order.getItems().stream()
                        .filter(item -> item.getSellerId().equals(sellerId))
                        .map(item -> new SellerProductStats.RecentSale(
                                order.getId(),
                                item.getProductId(),
                                item.getProductName(),
                                order.getUserName(),
                                item.getQuantity(),
                                item.getSubtotal(),
                                order.getDeliveredAt() != null ? order.getDeliveredAt() : order.getCreatedAt()
                        )))
                .limit(10)
                .collect(Collectors.toList());

        int totalUniqueProductsSold = productMap.size();
        int totalCustomers = uniqueCustomers.size();

        return new SellerProductStats(bestSelling, recentSales, totalUniqueProductsSold, totalCustomers);
    }

    // Stats DTOs
    public record UserOrderStats(long totalOrders, long completedOrders, double totalSpent) {}
    public record SellerOrderStats(long totalOrders, long completedOrders, double totalRevenue, long totalItemsSold) {}
}
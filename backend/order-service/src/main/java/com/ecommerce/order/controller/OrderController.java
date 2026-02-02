package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.SellerProductStats;
import com.ecommerce.order.dto.StatusUpdateRequest;
import com.ecommerce.order.dto.UserProductStats;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * Create a new order (checkout)
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody OrderRequest request,
            HttpServletRequest httpRequest) {
        try {
            String userId = (String) httpRequest.getAttribute("userId");
            String userName = (String) httpRequest.getAttribute("userName");
            String userEmail = (String) httpRequest.getAttribute("userEmail");

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            OrderResponse order = orderService.createOrder(request, userId, userName, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get order by ID
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        try {
            String userId = (String) httpRequest.getAttribute("userId");
            String role = (String) httpRequest.getAttribute("userRole");
            boolean isSeller = "SELLER".equals(role);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            OrderResponse order = orderService.getOrderById(id, userId, isSeller);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all orders for the authenticated user (customer)
     * GET /api/orders/my-orders
     */
    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            HttpServletRequest httpRequest) {
        try {
            String userId = (String) httpRequest.getAttribute("userId");

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            List<OrderResponse> orders;
            if (status != null) {
                orders = orderService.getOrdersByUserAndStatus(userId, status);
            } else {
                orders = orderService.getOrdersByUser(userId);
            }
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching user orders: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get orders for seller (orders containing their products)
     * GET /api/orders/seller
     */
    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getSellerOrders(
            @RequestParam(required = false) OrderStatus status,
            HttpServletRequest httpRequest) {
        try {
            String sellerId = (String) httpRequest.getAttribute("userId");

            if (sellerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            List<OrderResponse> orders;
            if (status != null) {
                orders = orderService.getOrdersForSellerByStatus(sellerId, status);
            } else {
                orders = orderService.getOrdersForSeller(sellerId);
            }
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching seller orders: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update order status (for sellers)
     * PUT /api/orders/{id}/status
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable String id,
            @Valid @RequestBody StatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        try {
            String sellerId = (String) httpRequest.getAttribute("userId");

            if (sellerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            OrderResponse order = orderService.updateOrderStatus(id, request, sellerId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", e.getMessage()));
            }
            if (e.getMessage().contains("Invalid status")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cancel order (for customers)
     * PUT /api/orders/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest httpRequest) {
        try {
            String userId = (String) httpRequest.getAttribute("userId");

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String reason = body != null ? body.get("reason") : null;
            OrderResponse order = orderService.cancelOrder(id, userId, reason);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", e.getMessage()));
            }
            if (e.getMessage().contains("cannot be cancelled")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reorder (create new order from existing)
     * POST /api/orders/{id}/reorder
     */
    @PostMapping("/{id}/reorder")
    public ResponseEntity<?> reorder(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        try {
            String userId = (String) httpRequest.getAttribute("userId");
            String userName = (String) httpRequest.getAttribute("userName");
            String userEmail = (String) httpRequest.getAttribute("userEmail");

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            OrderResponse order = orderService.reorder(id, userId, userName, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get user order statistics
     * GET /api/orders/stats/user
     */
    @GetMapping("/stats/user")
    public ResponseEntity<?> getUserStats(HttpServletRequest httpRequest) {
        try {
            String userId = (String) httpRequest.getAttribute("userId");

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            OrderService.UserOrderStats stats = orderService.getUserStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching user stats: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get seller order statistics
     * GET /api/orders/stats/seller
     */
    @GetMapping("/stats/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getSellerStats(HttpServletRequest httpRequest) {
        try {
            String sellerId = (String) httpRequest.getAttribute("userId");

            if (sellerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            OrderService.SellerOrderStats stats = orderService.getSellerStats(sellerId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching seller stats: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get detailed user product statistics (most purchased products)
     * GET /api/orders/stats/user/products
     */
    @GetMapping("/stats/user/products")
    public ResponseEntity<?> getUserProductStats(HttpServletRequest httpRequest) {
        try {
            String userId = (String) httpRequest.getAttribute("userId");

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            UserProductStats stats = orderService.getUserProductStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching user product stats: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get detailed seller product statistics (best selling products)
     * GET /api/orders/stats/seller/products
     */
    @GetMapping("/stats/seller/products")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getSellerProductStats(HttpServletRequest httpRequest) {
        try {
            String sellerId = (String) httpRequest.getAttribute("userId");

            if (sellerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            SellerProductStats stats = orderService.getSellerProductStats(sellerId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching seller product stats: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     * GET /api/orders/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "order-service"));
    }
}
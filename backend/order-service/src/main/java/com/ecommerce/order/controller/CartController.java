package com.ecommerce.order.controller;

import com.ecommerce.order.dto.CartItemRequest;
import com.ecommerce.order.dto.CartResponse;
import com.ecommerce.order.dto.UpdateQuantityRequest;
import com.ecommerce.order.security.JwtUtil;
import com.ecommerce.order.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final JwtUtil jwtUtil;

    /**
     * Get user's cart
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@RequestHeader("Authorization") String token) {
        String userId = extractUserId(token);
        log.info("Getting cart for user: {}", userId);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    /**
     * Add item to cart
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CartItemRequest request) {
        String userId = extractUserId(token);
        log.info("Adding item to cart for user: {}", userId);
        return ResponseEntity.ok(cartService.addToCart(userId, request));
    }

    /**
     * Update item quantity
     */
    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @RequestHeader("Authorization") String token,
            @PathVariable String productId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        String userId = extractUserId(token);
        log.info("Updating quantity for product {} in cart for user: {}", productId, userId);
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, productId, request.getQuantity()));
    }

    /**
     * Remove item from cart
     */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeFromCart(
            @RequestHeader("Authorization") String token,
            @PathVariable String productId) {
        String userId = extractUserId(token);
        log.info("Removing product {} from cart for user: {}", productId, userId);
        return ResponseEntity.ok(cartService.removeFromCart(userId, productId));
    }

    /**
     * Clear entire cart
     */
    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(@RequestHeader("Authorization") String token) {
        String userId = extractUserId(token);
        log.info("Clearing cart for user: {}", userId);
        return ResponseEntity.ok(cartService.clearCart(userId));
    }

    /**
     * Sync cart from frontend (replace entire cart with frontend state)
     */
    @PostMapping("/sync")
    public ResponseEntity<CartResponse> syncCart(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody List<CartItemRequest> items) {
        String userId = extractUserId(token);
        log.info("Syncing cart for user: {} with {} items", userId, items.size());
        return ResponseEntity.ok(cartService.syncCart(userId, items));
    }

    /**
     * Get cart item count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getCartCount(@RequestHeader("Authorization") String token) {
        String userId = extractUserId(token);
        int count = cartService.getCartItemCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Get cart total
     */
    @GetMapping("/total")
    public ResponseEntity<Map<String, Double>> getCartTotal(@RequestHeader("Authorization") String token) {
        String userId = extractUserId(token);
        double total = cartService.getCartTotal(userId);
        return ResponseEntity.ok(Map.of("total", total));
    }

    /**
     * Check if product is in cart
     */
    @GetMapping("/check/{productId}")
    public ResponseEntity<Map<String, Boolean>> isInCart(
            @RequestHeader("Authorization") String token,
            @PathVariable String productId) {
        String userId = extractUserId(token);
        boolean inCart = cartService.isProductInCart(userId, productId);
        return ResponseEntity.ok(Map.of("inCart", inCart));
    }

    private String extractUserId(String token) {
        String jwt = token.replace("Bearer ", "");
        return jwtUtil.extractUserId(jwt);
    }
}
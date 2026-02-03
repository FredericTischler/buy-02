package com.ecommerce.order.service;

import com.ecommerce.order.dto.CartItemRequest;
import com.ecommerce.order.dto.CartResponse;
import com.ecommerce.order.model.Cart;
import com.ecommerce.order.model.CartItem;
import com.ecommerce.order.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;

    /**
     * Get or create cart for a user
     */
    public CartResponse getCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));
        return CartResponse.fromCart(cart);
    }

    /**
     * Add item to cart
     */
    @Transactional
    public CartResponse addToCart(String userId, CartItemRequest request) {
        log.info("Adding item {} to cart for user {}", request.getProductId(), userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));

        CartItem newItem = new CartItem(
                request.getProductId(),
                request.getProductName(),
                request.getSellerId(),
                request.getSellerName(),
                request.getPrice(),
                request.getQuantity(),
                request.getStock(),
                request.getImageUrl()
        );

        cart.addItem(newItem);
        Cart savedCart = cartRepository.save(cart);

        log.info("Item added to cart. Cart now has {} items", savedCart.getTotalItems());
        return CartResponse.fromCart(savedCart);
    }

    /**
     * Update item quantity in cart
     */
    @Transactional
    public CartResponse updateItemQuantity(String userId, String productId, int quantity) {
        log.info("Updating quantity for product {} in cart for user {}", productId, userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        cart.updateItemQuantity(productId, quantity);
        Cart savedCart = cartRepository.save(cart);

        log.info("Item quantity updated. Cart now has {} items", savedCart.getTotalItems());
        return CartResponse.fromCart(savedCart);
    }

    /**
     * Remove item from cart
     */
    @Transactional
    public CartResponse removeFromCart(String userId, String productId) {
        log.info("Removing product {} from cart for user {}", productId, userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        cart.removeItem(productId);
        Cart savedCart = cartRepository.save(cart);

        log.info("Item removed from cart. Cart now has {} items", savedCart.getTotalItems());
        return CartResponse.fromCart(savedCart);
    }

    /**
     * Clear all items from cart
     */
    @Transactional
    public CartResponse clearCart(String userId) {
        log.info("Clearing cart for user {}", userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));

        cart.clear();
        Cart savedCart = cartRepository.save(cart);

        log.info("Cart cleared for user {}", userId);
        return CartResponse.fromCart(savedCart);
    }

    /**
     * Delete cart entirely (used after successful order)
     */
    @Transactional
    public void deleteCart(String userId) {
        log.info("Deleting cart for user {}", userId);
        cartRepository.deleteByUserId(userId);
    }

    /**
     * Get cart item count
     */
    public int getCartItemCount(String userId) {
        return cartRepository.findByUserId(userId)
                .map(Cart::getTotalItems)
                .orElse(0);
    }

    /**
     * Get cart total amount
     */
    public double getCartTotal(String userId) {
        return cartRepository.findByUserId(userId)
                .map(Cart::getTotalAmount)
                .orElse(0.0);
    }

    /**
     * Check if product is in cart
     */
    public boolean isProductInCart(String userId, String productId) {
        return cartRepository.findByUserId(userId)
                .map(cart -> cart.getItems().stream()
                        .anyMatch(item -> item.getProductId().equals(productId)))
                .orElse(false);
    }

    /**
     * Sync cart from frontend (replace entire cart)
     */
    @Transactional
    public CartResponse syncCart(String userId, java.util.List<CartItemRequest> items) {
        log.info("Syncing cart for user {} with {} items", userId, items.size());

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));

        // Clear and add all items
        cart.clear();
        for (CartItemRequest request : items) {
            CartItem item = new CartItem(
                    request.getProductId(),
                    request.getProductName(),
                    request.getSellerId(),
                    request.getSellerName(),
                    request.getPrice(),
                    request.getQuantity(),
                    request.getStock(),
                    request.getImageUrl()
            );
            cart.getItems().add(item);
        }
        cart.setUpdatedAt(LocalDateTime.now());

        Cart savedCart = cartRepository.save(cart);
        log.info("Cart synced. Cart now has {} items", savedCart.getTotalItems());
        return CartResponse.fromCart(savedCart);
    }

    // Helper method
    private Cart createEmptyCart(String userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setCreatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }
}
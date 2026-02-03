package com.ecommerce.order.service;

import com.ecommerce.order.dto.CartItemRequest;
import com.ecommerce.order.dto.CartResponse;
import com.ecommerce.order.model.Cart;
import com.ecommerce.order.model.CartItem;
import com.ecommerce.order.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    private CartService cartService;

    private static final String USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository);
    }

    // ==================== GET CART TESTS ====================

    @Test
    void getCart_shouldReturnExistingCart() {
        Cart cart = createCartWithItems();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getItems()).hasSize(1);
        verify(cartRepository).findByUserId(USER_ID);
    }

    @Test
    void getCart_shouldCreateEmptyCartForNewUser() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setId("cart-1");
            return cart;
        });

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getItems()).isEmpty();
        verify(cartRepository).save(any(Cart.class));
    }

    // ==================== ADD TO CART TESTS ====================

    @Test
    void addToCart_shouldAddNewItemToEmptyCart() {
        Cart emptyCart = createEmptyCart();
        CartItemRequest request = createCartItemRequest("prod-1", "Product 1", 29.99, 2);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addToCart(USER_ID, request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo("prod-1");
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void addToCart_shouldIncrementQuantityForExistingItem() {
        Cart cart = createCartWithItems();
        CartItemRequest request = createCartItemRequest("prod-1", "Product 1", 10.0, 3);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addToCart(USER_ID, request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5); // 2 + 3
    }

    @Test
    void addToCart_shouldCreateCartIfNotExists() {
        CartItemRequest request = createCartItemRequest("prod-1", "Product 1", 10.0, 1);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setId("new-cart");
            return cart;
        });

        CartResponse response = cartService.addToCart(USER_ID, request);

        assertThat(response.getItems()).hasSize(1);
        verify(cartRepository, times(2)).save(any(Cart.class)); // once for create, once for add
    }

    // ==================== UPDATE ITEM QUANTITY TESTS ====================

    @Test
    void updateItemQuantity_shouldUpdateQuantity() {
        Cart cart = createCartWithItems();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.updateItemQuantity(USER_ID, "prod-1", 5);

        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void updateItemQuantity_shouldRemoveItemWhenQuantityIsZero() {
        Cart cart = createCartWithItems();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.updateItemQuantity(USER_ID, "prod-1", 0);

        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void updateItemQuantity_shouldThrowWhenCartNotFound() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemQuantity(USER_ID, "prod-1", 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cart not found");
    }

    // ==================== REMOVE FROM CART TESTS ====================

    @Test
    void removeFromCart_shouldRemoveItem() {
        Cart cart = createCartWithItems();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.removeFromCart(USER_ID, "prod-1");

        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void removeFromCart_shouldThrowWhenCartNotFound() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeFromCart(USER_ID, "prod-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cart not found");
    }

    // ==================== CLEAR CART TESTS ====================

    @Test
    void clearCart_shouldRemoveAllItems() {
        Cart cart = createCartWithItems();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.clearCart(USER_ID);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalAmount()).isEqualTo(0.0);
    }

    // ==================== DELETE CART TESTS ====================

    @Test
    void deleteCart_shouldDeleteCartByUserId() {
        cartService.deleteCart(USER_ID);

        verify(cartRepository).deleteByUserId(USER_ID);
    }

    // ==================== GET CART ITEM COUNT TESTS ====================

    @Test
    void getCartItemCount_shouldReturnTotalQuantity() {
        Cart cart = createCartWithMultipleItems();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        int count = cartService.getCartItemCount(USER_ID);

        assertThat(count).isEqualTo(5); // 2 + 3
    }

    @Test
    void getCartItemCount_shouldReturnZeroWhenNoCart() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        int count = cartService.getCartItemCount(USER_ID);

        assertThat(count).isEqualTo(0);
    }

    // ==================== GET CART TOTAL TESTS ====================

    @Test
    void getCartTotal_shouldReturnTotalAmount() {
        Cart cart = createCartWithMultipleItems();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        double total = cartService.getCartTotal(USER_ID);

        assertThat(total).isEqualTo(80.0); // 10*2 + 20*3
    }

    @Test
    void getCartTotal_shouldReturnZeroWhenNoCart() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        double total = cartService.getCartTotal(USER_ID);

        assertThat(total).isEqualTo(0.0);
    }

    // ==================== IS PRODUCT IN CART TESTS ====================

    @Test
    void isProductInCart_shouldReturnTrueIfExists() {
        Cart cart = createCartWithItems();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        boolean result = cartService.isProductInCart(USER_ID, "prod-1");

        assertThat(result).isTrue();
    }

    @Test
    void isProductInCart_shouldReturnFalseIfNotExists() {
        Cart cart = createCartWithItems();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        boolean result = cartService.isProductInCart(USER_ID, "prod-999");

        assertThat(result).isFalse();
    }

    @Test
    void isProductInCart_shouldReturnFalseWhenNoCart() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        boolean result = cartService.isProductInCart(USER_ID, "prod-1");

        assertThat(result).isFalse();
    }

    // ==================== SYNC CART TESTS ====================

    @Test
    void syncCart_shouldReplaceEntireCart() {
        Cart existingCart = createCartWithItems();
        List<CartItemRequest> requests = List.of(
                createCartItemRequest("new-prod-1", "New Product 1", 15.0, 1),
                createCartItemRequest("new-prod-2", "New Product 2", 25.0, 2)
        );

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.syncCart(USER_ID, requests);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo("new-prod-1");
        assertThat(response.getItems().get(1).getProductId()).isEqualTo("new-prod-2");
    }

    @Test
    void syncCart_shouldCreateCartIfNotExists() {
        List<CartItemRequest> requests = List.of(
                createCartItemRequest("prod-1", "Product 1", 10.0, 1)
        );

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setId("new-cart");
            return cart;
        });

        CartResponse response = cartService.syncCart(USER_ID, requests);

        assertThat(response.getItems()).hasSize(1);
    }

    // ==================== HELPER METHODS ====================

    private Cart createEmptyCart() {
        Cart cart = new Cart();
        cart.setId("cart-1");
        cart.setUserId(USER_ID);
        cart.setItems(new ArrayList<>());
        cart.setCreatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());
        return cart;
    }

    private Cart createCartWithItems() {
        Cart cart = createEmptyCart();
        CartItem item = new CartItem("prod-1", "Product 1", "seller-1", "Seller", 10.0, 2, 100, null);
        cart.getItems().add(item);
        return cart;
    }

    private Cart createCartWithMultipleItems() {
        Cart cart = createEmptyCart();
        cart.getItems().add(new CartItem("prod-1", "Product 1", "seller-1", "Seller", 10.0, 2, 100, null));
        cart.getItems().add(new CartItem("prod-2", "Product 2", "seller-1", "Seller", 20.0, 3, 50, null));
        return cart;
    }

    private CartItemRequest createCartItemRequest(String productId, String name, double price, int quantity) {
        CartItemRequest request = new CartItemRequest();
        request.setProductId(productId);
        request.setProductName(name);
        request.setSellerId("seller-1");
        request.setSellerName("Test Seller");
        request.setPrice(price);
        request.setQuantity(quantity);
        request.setStock(100);
        request.setImageUrl(null);
        return request;
    }
}
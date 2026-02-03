package com.ecommerce.order.integration;

import com.ecommerce.order.dto.CartItemRequest;
import com.ecommerce.order.dto.CartResponse;
import com.ecommerce.order.repository.CartRepository;
import com.ecommerce.order.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class CartServiceIT extends BaseIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    private static final String TEST_USER_ID = "test-user-123";

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
    }

    @Test
    void getCart_ShouldCreateEmptyCartForNewUser() {
        CartResponse cart = cartService.getCart(TEST_USER_ID);

        assertNotNull(cart);
        assertEquals(TEST_USER_ID, cart.getUserId());
        assertTrue(cart.getItems().isEmpty());
        assertEquals(0.0, cart.getTotalAmount());
        assertEquals(0, cart.getTotalItems());
    }

    @Test
    void addToCart_ShouldAddNewItem() {
        CartItemRequest request = createCartItemRequest("product-1", "Test Product", 29.99, 2);

        CartResponse cart = cartService.addToCart(TEST_USER_ID, request);

        assertNotNull(cart);
        assertEquals(1, cart.getItems().size());
        assertEquals("product-1", cart.getItems().get(0).getProductId());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        assertEquals(59.98, cart.getTotalAmount(), 0.01);
    }

    @Test
    void addToCart_ShouldIncrementQuantityForExistingItem() {
        CartItemRequest request1 = createCartItemRequest("product-1", "Test Product", 10.0, 2);
        CartItemRequest request2 = createCartItemRequest("product-1", "Test Product", 10.0, 3);

        cartService.addToCart(TEST_USER_ID, request1);
        CartResponse cart = cartService.addToCart(TEST_USER_ID, request2);

        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).getQuantity());
        assertEquals(50.0, cart.getTotalAmount(), 0.01);
    }

    @Test
    void updateItemQuantity_ShouldUpdateQuantity() {
        CartItemRequest request = createCartItemRequest("product-1", "Test Product", 10.0, 2);
        cartService.addToCart(TEST_USER_ID, request);

        CartResponse cart = cartService.updateItemQuantity(TEST_USER_ID, "product-1", 5);

        assertEquals(5, cart.getItems().get(0).getQuantity());
        assertEquals(50.0, cart.getTotalAmount(), 0.01);
    }

    @Test
    void updateItemQuantity_ShouldRemoveItemWhenQuantityIsZero() {
        CartItemRequest request = createCartItemRequest("product-1", "Test Product", 10.0, 2);
        cartService.addToCart(TEST_USER_ID, request);

        CartResponse cart = cartService.updateItemQuantity(TEST_USER_ID, "product-1", 0);

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void removeFromCart_ShouldRemoveItem() {
        CartItemRequest request1 = createCartItemRequest("product-1", "Product 1", 10.0, 1);
        CartItemRequest request2 = createCartItemRequest("product-2", "Product 2", 20.0, 1);
        cartService.addToCart(TEST_USER_ID, request1);
        cartService.addToCart(TEST_USER_ID, request2);

        CartResponse cart = cartService.removeFromCart(TEST_USER_ID, "product-1");

        assertEquals(1, cart.getItems().size());
        assertEquals("product-2", cart.getItems().get(0).getProductId());
    }

    @Test
    void clearCart_ShouldRemoveAllItems() {
        CartItemRequest request1 = createCartItemRequest("product-1", "Product 1", 10.0, 1);
        CartItemRequest request2 = createCartItemRequest("product-2", "Product 2", 20.0, 1);
        cartService.addToCart(TEST_USER_ID, request1);
        cartService.addToCart(TEST_USER_ID, request2);

        CartResponse cart = cartService.clearCart(TEST_USER_ID);

        assertTrue(cart.getItems().isEmpty());
        assertEquals(0.0, cart.getTotalAmount());
    }

    @Test
    void getCartItemCount_ShouldReturnTotalQuantity() {
        CartItemRequest request1 = createCartItemRequest("product-1", "Product 1", 10.0, 2);
        CartItemRequest request2 = createCartItemRequest("product-2", "Product 2", 20.0, 3);
        cartService.addToCart(TEST_USER_ID, request1);
        cartService.addToCart(TEST_USER_ID, request2);

        int count = cartService.getCartItemCount(TEST_USER_ID);

        assertEquals(5, count);
    }

    @Test
    void isProductInCart_ShouldReturnTrueIfProductExists() {
        CartItemRequest request = createCartItemRequest("product-1", "Product 1", 10.0, 1);
        cartService.addToCart(TEST_USER_ID, request);

        assertTrue(cartService.isProductInCart(TEST_USER_ID, "product-1"));
        assertFalse(cartService.isProductInCart(TEST_USER_ID, "product-2"));
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
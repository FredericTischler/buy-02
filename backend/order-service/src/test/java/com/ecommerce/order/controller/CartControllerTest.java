package com.ecommerce.order.controller;

import com.ecommerce.order.dto.CartItemRequest;
import com.ecommerce.order.dto.CartResponse;
import com.ecommerce.order.dto.UpdateQuantityRequest;
import com.ecommerce.order.model.CartItem;
import com.ecommerce.order.security.JwtAuthenticationFilter;
import com.ecommerce.order.security.JwtUtil;
import com.ecommerce.order.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    MongoRepositoriesAutoConfiguration.class
})
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestMongoConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.data.mongodb.core.mapping.MongoMappingContext mongoMappingContext() {
            return new org.springframework.data.mongodb.core.mapping.MongoMappingContext();
        }
    }

    private static final String USER_ID = "user-123";
    private static final String AUTH_HEADER = "Bearer test-token";

    @BeforeEach
    void setUp() {
        Mockito.reset(cartService, jwtUtil);
        Mockito.when(jwtUtil.extractUserId("test-token")).thenReturn(USER_ID);
    }

    // ==================== GET CART TESTS ====================

    @Test
    void getCart_shouldReturnCart() throws Exception {
        CartResponse response = createSampleCartResponse();
        Mockito.when(cartService.getCart(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/cart")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(USER_ID))
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.totalItems").value(2));
    }

    // ==================== ADD TO CART TESTS ====================

    @Test
    void addToCart_shouldReturnUpdatedCart() throws Exception {
        CartItemRequest request = createCartItemRequest();
        CartResponse response = createSampleCartResponse();

        Mockito.when(cartService.addToCart(eq(USER_ID), any(CartItemRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(USER_ID))
            .andExpect(jsonPath("$.items").isArray());
    }

    // ==================== UPDATE QUANTITY TESTS ====================

    @Test
    void updateQuantity_shouldReturnUpdatedCart() throws Exception {
        UpdateQuantityRequest request = new UpdateQuantityRequest(5);
        CartResponse response = createSampleCartResponse();

        Mockito.when(cartService.updateItemQuantity(eq(USER_ID), eq("prod-1"), eq(5))).thenReturn(response);

        mockMvc.perform(put("/api/cart/items/prod-1")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    // ==================== REMOVE FROM CART TESTS ====================

    @Test
    void removeFromCart_shouldReturnUpdatedCart() throws Exception {
        CartResponse response = createSampleCartResponse();
        Mockito.when(cartService.removeFromCart(USER_ID, "prod-1")).thenReturn(response);

        mockMvc.perform(delete("/api/cart/items/prod-1")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    // ==================== CLEAR CART TESTS ====================

    @Test
    void clearCart_shouldReturnEmptyCart() throws Exception {
        CartResponse response = createEmptyCartResponse();
        Mockito.when(cartService.clearCart(USER_ID)).thenReturn(response);

        mockMvc.perform(delete("/api/cart")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isEmpty())
            .andExpect(jsonPath("$.totalItems").value(0));
    }

    // ==================== SYNC CART TESTS ====================

    @Test
    void syncCart_shouldReturnSyncedCart() throws Exception {
        List<CartItemRequest> items = List.of(createCartItemRequest());
        CartResponse response = createSampleCartResponse();

        Mockito.when(cartService.syncCart(eq(USER_ID), anyList())).thenReturn(response);

        mockMvc.perform(post("/api/cart/sync")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(items)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    // ==================== GET CART COUNT TESTS ====================

    @Test
    void getCartCount_shouldReturnCount() throws Exception {
        Mockito.when(cartService.getCartItemCount(USER_ID)).thenReturn(5);

        mockMvc.perform(get("/api/cart/count")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(5));
    }

    // ==================== GET CART TOTAL TESTS ====================

    @Test
    void getCartTotal_shouldReturnTotal() throws Exception {
        Mockito.when(cartService.getCartTotal(USER_ID)).thenReturn(99.99);

        mockMvc.perform(get("/api/cart/total")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(99.99));
    }

    // ==================== IS IN CART TESTS ====================

    @Test
    void isInCart_shouldReturnTrue() throws Exception {
        Mockito.when(cartService.isProductInCart(USER_ID, "prod-1")).thenReturn(true);

        mockMvc.perform(get("/api/cart/check/prod-1")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inCart").value(true));
    }

    @Test
    void isInCart_shouldReturnFalse() throws Exception {
        Mockito.when(cartService.isProductInCart(USER_ID, "prod-999")).thenReturn(false);

        mockMvc.perform(get("/api/cart/check/prod-999")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inCart").value(false));
    }

    // ==================== HELPER METHODS ====================

    private CartResponse createSampleCartResponse() {
        CartResponse response = new CartResponse();
        response.setId("cart-1");
        response.setUserId(USER_ID);
        List<CartItem> items = new ArrayList<>();
        items.add(new CartItem("prod-1", "Product 1", "seller-1", "Seller", 10.0, 2, 100, null));
        response.setItems(items);
        response.setTotalAmount(20.0);
        response.setTotalItems(2);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    private CartResponse createEmptyCartResponse() {
        CartResponse response = new CartResponse();
        response.setId("cart-1");
        response.setUserId(USER_ID);
        response.setItems(new ArrayList<>());
        response.setTotalAmount(0.0);
        response.setTotalItems(0);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    private CartItemRequest createCartItemRequest() {
        CartItemRequest request = new CartItemRequest();
        request.setProductId("prod-1");
        request.setProductName("Product 1");
        request.setSellerId("seller-1");
        request.setSellerName("Test Seller");
        request.setPrice(10.0);
        request.setQuantity(2);
        request.setStock(100);
        request.setImageUrl(null);
        return request;
    }
}
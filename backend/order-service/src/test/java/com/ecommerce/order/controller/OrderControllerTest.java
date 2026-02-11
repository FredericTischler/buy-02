package com.ecommerce.order.controller;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.security.JwtAuthenticationFilter;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    MongoRepositoriesAutoConfiguration.class
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestMongoConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.data.mongodb.core.mapping.MongoMappingContext mongoMappingContext() {
            return new org.springframework.data.mongodb.core.mapping.MongoMappingContext();
        }
    }

    // ==================== CREATE ORDER TESTS ====================

    @Test
    void createOrder_shouldReturnCreatedOrder() throws Exception {
        OrderRequest request = createValidOrderRequest();
        OrderResponse response = createSampleOrderResponse("order-123", "user-1");

        Mockito.when(orderService.createOrder(any(OrderRequest.class), eq("user-1"), eq("John"), eq("john@mail.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", "user-1")
                .requestAttr("userName", "John")
                .requestAttr("userEmail", "john@mail.com"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("order-123"))
            .andExpect(jsonPath("$.userId").value("user-1"))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createOrder_shouldReturnUnauthorizedWhenNoUserId() throws Exception {
        OrderRequest request = createValidOrderRequest();

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("User not authenticated"));
    }

    @Test
    void createOrder_shouldHandleServiceError() throws Exception {
        OrderRequest request = createValidOrderRequest();

        Mockito.when(orderService.createOrder(any(OrderRequest.class), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Insufficient stock"));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", "user-1")
                .requestAttr("userName", "John")
                .requestAttr("userEmail", "john@mail.com"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Insufficient stock"));
    }

    // ==================== GET ORDER BY ID TESTS ====================

    @Test
    void getOrderById_shouldReturnOrder() throws Exception {
        OrderResponse response = createSampleOrderResponse("order-123", "user-1");

        Mockito.when(orderService.getOrderById("order-123", "user-1", false))
                .thenReturn(response);

        mockMvc.perform(get("/api/orders/order-123")
                .requestAttr("userId", "user-1")
                .requestAttr("userRole", "CLIENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("order-123"))
            .andExpect(jsonPath("$.userId").value("user-1"));
    }

    @Test
    void getOrderById_shouldReturnOrderForSeller() throws Exception {
        OrderResponse response = createSampleOrderResponse("order-123", "user-1");

        Mockito.when(orderService.getOrderById("order-123", "seller-1", true))
                .thenReturn(response);

        mockMvc.perform(get("/api/orders/order-123")
                .requestAttr("userId", "seller-1")
                .requestAttr("userRole", "SELLER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("order-123"));
    }

    @Test
    void getOrderById_shouldReturnUnauthorizedWhenNoUserId() throws Exception {
        mockMvc.perform(get("/api/orders/order-123"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("User not authenticated"));
    }

    @Test
    void getOrderById_shouldReturnNotFoundWhenOrderMissing() throws Exception {
        Mockito.when(orderService.getOrderById("order-123", "user-1", false))
                .thenThrow(new RuntimeException("Order not found"));

        mockMvc.perform(get("/api/orders/order-123")
                .requestAttr("userId", "user-1")
                .requestAttr("userRole", "CLIENT"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Order not found"));
    }

    @Test
    void getOrderById_shouldReturnForbiddenWhenNotAuthorized() throws Exception {
        Mockito.when(orderService.getOrderById("order-123", "other-user", false))
                .thenThrow(new RuntimeException("You are not authorized to view this order"));

        mockMvc.perform(get("/api/orders/order-123")
                .requestAttr("userId", "other-user")
                .requestAttr("userRole", "CLIENT"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("You are not authorized to view this order"));
    }

    // ==================== GET MY ORDERS TESTS ====================

    @Test
    void getMyOrders_shouldReturnUserOrders() throws Exception {
        List<OrderResponse> orders = Arrays.asList(
                createSampleOrderResponse("order-1", "user-1"),
                createSampleOrderResponse("order-2", "user-1")
        );

        Mockito.when(orderService.getOrdersByUser("user-1")).thenReturn(orders);

        mockMvc.perform(get("/api/orders/my-orders")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value("order-1"))
            .andExpect(jsonPath("$[1].id").value("order-2"));
    }

    @Test
    void getMyOrders_shouldReturnFilteredOrdersByStatus() throws Exception {
        OrderResponse order = createSampleOrderResponse("order-1", "user-1");
        order.setStatus(OrderStatus.SHIPPED);

        Mockito.when(orderService.getOrdersByUserAndStatus("user-1", OrderStatus.SHIPPED))
                .thenReturn(Collections.singletonList(order));

        mockMvc.perform(get("/api/orders/my-orders")
                .param("status", "SHIPPED")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("SHIPPED"));
    }

    @Test
    void getMyOrders_shouldReturnUnauthorizedWhenNoUserId() throws Exception {
        mockMvc.perform(get("/api/orders/my-orders"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("User not authenticated"));
    }

    @Test
    void getMyOrders_shouldReturnEmptyListWhenNoOrders() throws Exception {
        Mockito.when(orderService.getOrdersByUser("user-1")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/orders/my-orders")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== GET SELLER ORDERS TESTS ====================

    @Test
    void getSellerOrders_shouldReturnSellerOrders() throws Exception {
        List<OrderResponse> orders = Arrays.asList(
                createSampleOrderResponse("order-1", "user-1"),
                createSampleOrderResponse("order-2", "user-2")
        );

        Mockito.when(orderService.getOrdersForSeller("seller-1")).thenReturn(orders);

        mockMvc.perform(get("/api/orders/seller")
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getSellerOrders_shouldReturnFilteredOrdersByStatus() throws Exception {
        OrderResponse order = createSampleOrderResponse("order-1", "user-1");
        order.setStatus(OrderStatus.CONFIRMED);

        Mockito.when(orderService.getOrdersForSellerByStatus("seller-1", OrderStatus.CONFIRMED))
                .thenReturn(Collections.singletonList(order));

        mockMvc.perform(get("/api/orders/seller")
                .param("status", "CONFIRMED")
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void getSellerOrders_shouldReturnUnauthorizedWhenNoUserId() throws Exception {
        mockMvc.perform(get("/api/orders/seller"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("User not authenticated"));
    }

    // ==================== UPDATE ORDER STATUS TESTS ====================

    @Test
    void updateOrderStatus_shouldUpdateStatus() throws Exception {
        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.CONFIRMED, null);
        OrderResponse response = createSampleOrderResponse("order-123", "user-1");
        response.setStatus(OrderStatus.CONFIRMED);

        Mockito.when(orderService.updateOrderStatus(eq("order-123"), any(StatusUpdateRequest.class), eq("seller-1")))
                .thenReturn(response);

        mockMvc.perform(put("/api/orders/order-123/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("order-123"))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void updateOrderStatus_shouldReturnUnauthorizedWhenNoUserId() throws Exception {
        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.CONFIRMED, null);

        mockMvc.perform(put("/api/orders/order-123/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("User not authenticated"));
    }

    @Test
    void updateOrderStatus_shouldReturnNotFoundWhenOrderMissing() throws Exception {
        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.CONFIRMED, null);

        Mockito.when(orderService.updateOrderStatus(eq("order-123"), any(StatusUpdateRequest.class), eq("seller-1")))
                .thenThrow(new RuntimeException("Order not found"));

        mockMvc.perform(put("/api/orders/order-123/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Order not found"));
    }

    @Test
    void updateOrderStatus_shouldReturnForbiddenWhenNotAuthorized() throws Exception {
        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.CONFIRMED, null);

        Mockito.when(orderService.updateOrderStatus(eq("order-123"), any(StatusUpdateRequest.class), eq("other-seller")))
                .thenThrow(new RuntimeException("You are not authorized to update this order"));

        mockMvc.perform(put("/api/orders/order-123/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", "other-seller"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("You are not authorized to update this order"));
    }

    @Test
    void updateOrderStatus_shouldReturnBadRequestOnInvalidTransition() throws Exception {
        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.SHIPPED, null);

        Mockito.when(orderService.updateOrderStatus(eq("order-123"), any(StatusUpdateRequest.class), eq("seller-1")))
                .thenThrow(new RuntimeException("Invalid status transition from PENDING to SHIPPED"));

        mockMvc.perform(put("/api/orders/order-123/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid status transition from PENDING to SHIPPED"));
    }

    // ==================== CANCEL ORDER TESTS ====================

    @Test
    void cancelOrder_shouldCancelOrder() throws Exception {
        OrderResponse response = createSampleOrderResponse("order-123", "user-1");
        response.setStatus(OrderStatus.CANCELLED);
        response.setCancellationReason("Changed my mind");

        Mockito.when(orderService.cancelOrder("order-123", "user-1", "Changed my mind"))
                .thenReturn(response);

        mockMvc.perform(put("/api/orders/order-123/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"Changed my mind\"}")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("order-123"))
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.cancellationReason").value("Changed my mind"));
    }

    @Test
    void cancelOrder_shouldCancelOrderWithoutReason() throws Exception {
        OrderResponse response = createSampleOrderResponse("order-123", "user-1");
        response.setStatus(OrderStatus.CANCELLED);

        Mockito.when(orderService.cancelOrder("order-123", "user-1", null))
                .thenReturn(response);

        mockMvc.perform(put("/api/orders/order-123/cancel")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_shouldReturnUnauthorizedWhenNoUserId() throws Exception {
        mockMvc.perform(put("/api/orders/order-123/cancel"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("User not authenticated"));
    }

    @Test
    void cancelOrder_shouldReturnNotFoundWhenOrderMissing() throws Exception {
        Mockito.when(orderService.cancelOrder("order-123", "user-1", null))
                .thenThrow(new RuntimeException("Order not found"));

        mockMvc.perform(put("/api/orders/order-123/cancel")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Order not found"));
    }

    @Test
    void cancelOrder_shouldReturnForbiddenWhenNotAuthorized() throws Exception {
        Mockito.when(orderService.cancelOrder("order-123", "other-user", null))
                .thenThrow(new RuntimeException("You are not authorized to cancel this order"));

        mockMvc.perform(put("/api/orders/order-123/cancel")
                .requestAttr("userId", "other-user"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("You are not authorized to cancel this order"));
    }

    @Test
    void cancelOrder_shouldReturnBadRequestWhenOrderCannotBeCancelled() throws Exception {
        Mockito.when(orderService.cancelOrder("order-123", "user-1", null))
                .thenThrow(new RuntimeException("Order cannot be cancelled in current status: SHIPPED"));

        mockMvc.perform(put("/api/orders/order-123/cancel")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Order cannot be cancelled in current status: SHIPPED"));
    }

    // ==================== REORDER TESTS ====================

    @Test
    void reorder_shouldCreateNewOrder() throws Exception {
        OrderResponse response = createSampleOrderResponse("order-456", "user-1");

        Mockito.when(orderService.reorder("order-123", "user-1", "John", "john@mail.com"))
                .thenReturn(response);

        mockMvc.perform(post("/api/orders/order-123/reorder")
                .requestAttr("userId", "user-1")
                .requestAttr("userName", "John")
                .requestAttr("userEmail", "john@mail.com"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("order-456"))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void reorder_shouldReturnUnauthorizedWhenNoUserId() throws Exception {
        mockMvc.perform(post("/api/orders/order-123/reorder"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("User not authenticated"));
    }

    @Test
    void reorder_shouldReturnNotFoundWhenOrderMissing() throws Exception {
        Mockito.when(orderService.reorder("order-123", "user-1", "John", "john@mail.com"))
                .thenThrow(new RuntimeException("Order not found"));

        mockMvc.perform(post("/api/orders/order-123/reorder")
                .requestAttr("userId", "user-1")
                .requestAttr("userName", "John")
                .requestAttr("userEmail", "john@mail.com"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Order not found"));
    }

    @Test
    void reorder_shouldReturnForbiddenWhenNotAuthorized() throws Exception {
        Mockito.when(orderService.reorder("order-123", "other-user", "Other", "other@mail.com"))
                .thenThrow(new RuntimeException("You are not authorized to reorder this order"));

        mockMvc.perform(post("/api/orders/order-123/reorder")
                .requestAttr("userId", "other-user")
                .requestAttr("userName", "Other")
                .requestAttr("userEmail", "other@mail.com"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("You are not authorized to reorder this order"));
    }

    // ==================== USER STATS TESTS ====================

    @Test
    void getUserStats_shouldReturnStats() throws Exception {
        OrderService.UserOrderStats stats = new OrderService.UserOrderStats(10, 8, 500.0);

        Mockito.when(orderService.getUserStats("user-1")).thenReturn(stats);

        mockMvc.perform(get("/api/orders/stats/user")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalOrders").value(10))
            .andExpect(jsonPath("$.completedOrders").value(8))
            .andExpect(jsonPath("$.totalSpent").value(500.0));
    }

    @Test
    void getUserStats_shouldReturnUnauthorizedWhenNoUserId() throws Exception {
        mockMvc.perform(get("/api/orders/stats/user"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("User not authenticated"));
    }

    // ==================== SELLER STATS TESTS ====================

    @Test
    void getSellerStats_shouldReturnStats() throws Exception {
        OrderService.SellerOrderStats stats = new OrderService.SellerOrderStats(20, 15, 2500.0, 50);

        Mockito.when(orderService.getSellerStats("seller-1")).thenReturn(stats);

        mockMvc.perform(get("/api/orders/stats/seller")
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalOrders").value(20))
            .andExpect(jsonPath("$.completedOrders").value(15))
            .andExpect(jsonPath("$.totalRevenue").value(2500.0))
            .andExpect(jsonPath("$.totalItemsSold").value(50));
    }

    @Test
    void getSellerStats_shouldReturnUnauthorizedWhenNoUserId() throws Exception {
        mockMvc.perform(get("/api/orders/stats/seller"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("User not authenticated"));
    }

    // ==================== SEARCH MY ORDERS TESTS ====================

    @Test
    void searchMyOrders_shouldReturnFilteredOrders() throws Exception {
        List<OrderResponse> orders = Collections.singletonList(
                createSampleOrderResponse("order-1", "user-1"));

        Mockito.when(orderService.searchOrdersByUser(eq("user-1"), any(OrderSearchParams.class)))
                .thenReturn(orders);

        mockMvc.perform(get("/api/orders/my-orders/search")
                .param("keyword", "test")
                .param("sortBy", "createdAt")
                .param("sortDir", "desc")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void searchMyOrders_shouldReturnUnauthorizedWhenNoUserId() throws Exception {
        mockMvc.perform(get("/api/orders/my-orders/search"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("User not authenticated"));
    }

    @Test
    void searchMyOrders_shouldHandleServiceError() throws Exception {
        Mockito.when(orderService.searchOrdersByUser(eq("user-1"), any(OrderSearchParams.class)))
                .thenThrow(new RuntimeException("Search failed"));

        mockMvc.perform(get("/api/orders/my-orders/search")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Search failed"));
    }

    // ==================== SEARCH SELLER ORDERS TESTS ====================

    @Test
    void searchSellerOrders_shouldReturnFilteredOrders() throws Exception {
        List<OrderResponse> orders = Collections.singletonList(
                createSampleOrderResponse("order-1", "user-1"));

        Mockito.when(orderService.searchOrdersForSeller(eq("seller-1"), any(OrderSearchParams.class)))
                .thenReturn(orders);

        mockMvc.perform(get("/api/orders/seller/search")
                .param("keyword", "test")
                .param("sortBy", "createdAt")
                .param("sortDir", "desc")
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void searchSellerOrders_shouldReturnUnauthorizedWhenNoUserId() throws Exception {
        mockMvc.perform(get("/api/orders/seller/search"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("User not authenticated"));
    }

    @Test
    void searchSellerOrders_shouldHandleServiceError() throws Exception {
        Mockito.when(orderService.searchOrdersForSeller(eq("seller-1"), any(OrderSearchParams.class)))
                .thenThrow(new RuntimeException("Search failed"));

        mockMvc.perform(get("/api/orders/seller/search")
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Search failed"));
    }

    // ==================== USER PRODUCT STATS TESTS ====================

    @Test
    void getUserProductStats_shouldReturnStats() throws Exception {
        UserProductStats stats = new UserProductStats();
        stats.setMostPurchasedProducts(Collections.emptyList());
        stats.setTopCategories(Collections.emptyList());
        stats.setTotalUniqueProducts(5);
        stats.setTotalItemsPurchased(20);

        Mockito.when(orderService.getUserProductStats("user-1")).thenReturn(stats);

        mockMvc.perform(get("/api/orders/stats/user/products")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalUniqueProducts").value(5))
            .andExpect(jsonPath("$.totalItemsPurchased").value(20));
    }

    @Test
    void getUserProductStats_shouldReturnUnauthorizedWhenNoUserId() throws Exception {
        mockMvc.perform(get("/api/orders/stats/user/products"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("User not authenticated"));
    }

    @Test
    void getUserProductStats_shouldHandleServiceError() throws Exception {
        Mockito.when(orderService.getUserProductStats("user-1"))
                .thenThrow(new RuntimeException("Stats error"));

        mockMvc.perform(get("/api/orders/stats/user/products")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Stats error"));
    }

    // ==================== SELLER PRODUCT STATS TESTS ====================

    @Test
    void getSellerProductStats_shouldReturnStats() throws Exception {
        SellerProductStats stats = new SellerProductStats();
        stats.setBestSellingProducts(Collections.emptyList());
        stats.setRecentSales(Collections.emptyList());
        stats.setTotalUniqueProductsSold(10);
        stats.setTotalCustomers(8);

        Mockito.when(orderService.getSellerProductStats("seller-1")).thenReturn(stats);

        mockMvc.perform(get("/api/orders/stats/seller/products")
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalUniqueProductsSold").value(10))
            .andExpect(jsonPath("$.totalCustomers").value(8));
    }

    @Test
    void getSellerProductStats_shouldReturnUnauthorizedWhenNoUserId() throws Exception {
        mockMvc.perform(get("/api/orders/stats/seller/products"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("User not authenticated"));
    }

    @Test
    void getSellerProductStats_shouldHandleServiceError() throws Exception {
        Mockito.when(orderService.getSellerProductStats("seller-1"))
                .thenThrow(new RuntimeException("Stats error"));

        mockMvc.perform(get("/api/orders/stats/seller/products")
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Stats error"));
    }

    // ==================== DELETE ORDER TESTS ====================

    @Test
    void deleteOrder_shouldDeleteOrder() throws Exception {
        Mockito.doNothing().when(orderService).deleteOrder("order-123", "user-1");

        mockMvc.perform(delete("/api/orders/order-123")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Order deleted successfully"));
    }

    @Test
    void deleteOrder_shouldReturnUnauthorizedWhenNoUserId() throws Exception {
        mockMvc.perform(delete("/api/orders/order-123"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("User not authenticated"));
    }

    @Test
    void deleteOrder_shouldReturnNotFoundWhenOrderMissing() throws Exception {
        Mockito.doThrow(new RuntimeException("Order not found"))
                .when(orderService).deleteOrder("order-123", "user-1");

        mockMvc.perform(delete("/api/orders/order-123")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Order not found"));
    }

    @Test
    void deleteOrder_shouldReturnForbiddenWhenNotAuthorized() throws Exception {
        Mockito.doThrow(new RuntimeException("You are not authorized to delete this order"))
                .when(orderService).deleteOrder("order-123", "other-user");

        mockMvc.perform(delete("/api/orders/order-123")
                .requestAttr("userId", "other-user"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("You are not authorized to delete this order"));
    }

    @Test
    void deleteOrder_shouldReturnBadRequestWhenOrderCannotBeDeleted() throws Exception {
        Mockito.doThrow(new RuntimeException("Order cannot be deleted in current status"))
                .when(orderService).deleteOrder("order-123", "user-1");

        mockMvc.perform(delete("/api/orders/order-123")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Order cannot be deleted in current status"));
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    void getMyOrders_shouldHandleServiceError() throws Exception {
        Mockito.when(orderService.getOrdersByUser("user-1"))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/orders/my-orders")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Database error"));
    }

    @Test
    void getSellerOrders_shouldHandleServiceError() throws Exception {
        Mockito.when(orderService.getOrdersForSeller("seller-1"))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/orders/seller")
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Database error"));
    }

    @Test
    void getUserStats_shouldHandleServiceError() throws Exception {
        Mockito.when(orderService.getUserStats("user-1"))
                .thenThrow(new RuntimeException("Stats error"));

        mockMvc.perform(get("/api/orders/stats/user")
                .requestAttr("userId", "user-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Stats error"));
    }

    @Test
    void getSellerStats_shouldHandleServiceError() throws Exception {
        Mockito.when(orderService.getSellerStats("seller-1"))
                .thenThrow(new RuntimeException("Stats error"));

        mockMvc.perform(get("/api/orders/stats/seller")
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Stats error"));
    }

    // ==================== HEALTH CHECK TEST ====================

    @Test
    void health_shouldReturnStatus() throws Exception {
        mockMvc.perform(get("/api/orders/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("order-service"));
    }

    // ==================== HELPER METHODS ====================

    private OrderRequest createValidOrderRequest() {
        OrderItemRequest item1 = new OrderItemRequest("prod-1", "Product 1", "seller-1", "Seller", 50.0, 2, null);
        OrderItemRequest item2 = new OrderItemRequest("prod-2", "Product 2", "seller-1", "Seller", 25.0, 2, null);

        OrderRequest request = new OrderRequest();
        request.setItems(Arrays.asList(item1, item2));
        request.setShippingAddress("123 Main St");
        request.setShippingCity("Paris");
        request.setShippingPostalCode("75001");
        request.setShippingCountry("France");
        request.setPhoneNumber("+33123456789");
        request.setPaymentMethod("COD");
        return request;
    }

    private OrderResponse createSampleOrderResponse(String orderId, String userId) {
        OrderResponse response = new OrderResponse();
        response.setId(orderId);
        response.setUserId(userId);
        response.setUserName("Test User");
        response.setUserEmail("test@mail.com");
        response.setStatus(OrderStatus.PENDING);
        response.setTotalAmount(150.0);
        response.setShippingAddress("123 Main St");
        response.setShippingCity("Paris");
        response.setShippingPostalCode("75001");
        response.setShippingCountry("France");
        response.setPhoneNumber("+33123456789");
        response.setPaymentMethod("COD");
        response.setCreatedAt(LocalDateTime.now());
        response.setItems(Arrays.asList(
                new OrderItem("prod-1", "Product 1", "seller-1", "Seller", 50.0, 2, null),
                new OrderItem("prod-2", "Product 2", "seller-1", "Seller", 25.0, 2, null)
        ));
        return response;
    }
}
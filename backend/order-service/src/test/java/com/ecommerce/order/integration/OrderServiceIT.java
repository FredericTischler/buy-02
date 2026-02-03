package com.ecommerce.order.integration;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceIT extends BaseIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    private static final String TEST_USER_ID = "user-123";
    private static final String TEST_USER_NAME = "John Doe";
    private static final String TEST_USER_EMAIL = "john@example.com";
    private static final String TEST_SELLER_ID = "seller-456";

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void createOrder_ShouldCreateOrderSuccessfully() {
        OrderRequest request = createOrderRequest();

        OrderResponse response = orderService.createOrder(request, TEST_USER_ID, TEST_USER_NAME, TEST_USER_EMAIL);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(TEST_USER_ID, response.getUserId());
        assertEquals(TEST_USER_NAME, response.getUserName());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(2, response.getItems().size());
        assertEquals(79.98, response.getTotalAmount(), 0.01);
    }

    @Test
    void getOrderById_ShouldReturnOrderForOwner() {
        OrderRequest request = createOrderRequest();
        OrderResponse created = orderService.createOrder(request, TEST_USER_ID, TEST_USER_NAME, TEST_USER_EMAIL);

        OrderResponse found = orderService.getOrderById(created.getId(), TEST_USER_ID, false);

        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
    }

    @Test
    void getOrderById_ShouldThrowExceptionForUnauthorizedUser() {
        OrderRequest request = createOrderRequest();
        OrderResponse created = orderService.createOrder(request, TEST_USER_ID, TEST_USER_NAME, TEST_USER_EMAIL);

        assertThrows(RuntimeException.class, () ->
            orderService.getOrderById(created.getId(), "other-user", false)
        );
    }

    @Test
    void getOrdersByUser_ShouldReturnUserOrders() {
        OrderRequest request1 = createOrderRequest();
        OrderRequest request2 = createOrderRequest();
        orderService.createOrder(request1, TEST_USER_ID, TEST_USER_NAME, TEST_USER_EMAIL);
        orderService.createOrder(request2, TEST_USER_ID, TEST_USER_NAME, TEST_USER_EMAIL);
        orderService.createOrder(request1, "other-user", "Other", "other@test.com");

        List<OrderResponse> orders = orderService.getOrdersByUser(TEST_USER_ID);

        assertEquals(2, orders.size());
        assertTrue(orders.stream().allMatch(o -> o.getUserId().equals(TEST_USER_ID)));
    }

    @Test
    void cancelOrder_ShouldCancelPendingOrder() {
        OrderRequest request = createOrderRequest();
        OrderResponse created = orderService.createOrder(request, TEST_USER_ID, TEST_USER_NAME, TEST_USER_EMAIL);

        OrderResponse cancelled = orderService.cancelOrder(created.getId(), TEST_USER_ID, "Changed my mind");

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());
        assertEquals("Changed my mind", cancelled.getCancellationReason());
    }

    @Test
    void cancelOrder_ShouldThrowExceptionForNonPendingOrder() {
        OrderRequest request = createOrderRequest();
        OrderResponse created = orderService.createOrder(request, TEST_USER_ID, TEST_USER_NAME, TEST_USER_EMAIL);

        // First update status to SHIPPED
        StatusUpdateRequest statusUpdate = new StatusUpdateRequest();
        statusUpdate.setStatus(OrderStatus.CONFIRMED);
        orderService.updateOrderStatus(created.getId(), statusUpdate, TEST_SELLER_ID);

        statusUpdate.setStatus(OrderStatus.SHIPPED);
        orderService.updateOrderStatus(created.getId(), statusUpdate, TEST_SELLER_ID);

        assertThrows(RuntimeException.class, () ->
            orderService.cancelOrder(created.getId(), TEST_USER_ID, "Too late")
        );
    }

    @Test
    void reorder_ShouldCreateNewOrderFromExisting() {
        OrderRequest request = createOrderRequest();
        OrderResponse original = orderService.createOrder(request, TEST_USER_ID, TEST_USER_NAME, TEST_USER_EMAIL);

        OrderResponse reordered = orderService.reorder(original.getId(), TEST_USER_ID, TEST_USER_NAME, TEST_USER_EMAIL);

        assertNotNull(reordered);
        assertNotEquals(original.getId(), reordered.getId());
        assertEquals(original.getItems().size(), reordered.getItems().size());
        assertEquals(original.getTotalAmount(), reordered.getTotalAmount());
        assertEquals(OrderStatus.PENDING, reordered.getStatus());
    }

    @Test
    void getUserStats_ShouldReturnCorrectStatistics() {
        OrderRequest request = createOrderRequest();
        OrderResponse order = orderService.createOrder(request, TEST_USER_ID, TEST_USER_NAME, TEST_USER_EMAIL);

        // Update to DELIVERED
        StatusUpdateRequest statusUpdate = new StatusUpdateRequest();
        statusUpdate.setStatus(OrderStatus.CONFIRMED);
        orderService.updateOrderStatus(order.getId(), statusUpdate, TEST_SELLER_ID);
        statusUpdate.setStatus(OrderStatus.SHIPPED);
        orderService.updateOrderStatus(order.getId(), statusUpdate, TEST_SELLER_ID);
        statusUpdate.setStatus(OrderStatus.DELIVERED);
        orderService.updateOrderStatus(order.getId(), statusUpdate, TEST_SELLER_ID);

        OrderService.UserOrderStats stats = orderService.getUserStats(TEST_USER_ID);

        assertEquals(1, stats.totalOrders());
        assertEquals(1, stats.completedOrders());
        assertEquals(79.98, stats.totalSpent(), 0.01);
    }

    @Test
    void deleteOrder_ShouldDeleteCancelledOrder() {
        OrderRequest request = createOrderRequest();
        OrderResponse created = orderService.createOrder(request, TEST_USER_ID, TEST_USER_NAME, TEST_USER_EMAIL);
        orderService.cancelOrder(created.getId(), TEST_USER_ID, "Test");

        assertDoesNotThrow(() -> orderService.deleteOrder(created.getId(), TEST_USER_ID));
        assertTrue(orderRepository.findById(created.getId()).isEmpty());
    }

    @Test
    void deleteOrder_ShouldThrowExceptionForActiveOrder() {
        OrderRequest request = createOrderRequest();
        OrderResponse created = orderService.createOrder(request, TEST_USER_ID, TEST_USER_NAME, TEST_USER_EMAIL);

        assertThrows(RuntimeException.class, () ->
            orderService.deleteOrder(created.getId(), TEST_USER_ID)
        );
    }

    private OrderRequest createOrderRequest() {
        OrderRequest request = new OrderRequest();

        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId("product-1");
        item1.setProductName("Product 1");
        item1.setSellerId(TEST_SELLER_ID);
        item1.setSellerName("Test Seller");
        item1.setPrice(29.99);
        item1.setQuantity(2);
        item1.setImageUrl(null);

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId("product-2");
        item2.setProductName("Product 2");
        item2.setSellerId(TEST_SELLER_ID);
        item2.setSellerName("Test Seller");
        item2.setPrice(20.00);
        item2.setQuantity(1);
        item2.setImageUrl(null);

        request.setItems(List.of(item1, item2));
        request.setShippingAddress("123 Test Street");
        request.setShippingCity("Paris");
        request.setShippingPostalCode("75001");
        request.setShippingCountry("France");
        request.setPhoneNumber("+33612345678");
        request.setPaymentMethod("COD");
        request.setNotes("Test order");

        return request;
    }
}
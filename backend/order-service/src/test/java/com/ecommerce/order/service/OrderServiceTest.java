package com.ecommerce.order.service;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, kafkaTemplate);
    }

    // ==================== CREATE ORDER TESTS ====================

    @Test
    void createOrder_shouldCreateOrderSuccessfully() {
        OrderRequest request = createValidOrderRequest();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-123");
            return order;
        });

        OrderResponse response = orderService.createOrder(request, "user-1", "John Doe", "john@mail.com");

        assertThat(response.getId()).isEqualTo("order-123");
        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getUserName()).isEqualTo("John Doe");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualTo(150.0); // 50*2 + 25*2
        assertThat(response.getShippingCity()).isEqualTo("Paris");

        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(eq("order-events"), eq("order-123"), any(OrderEvent.class));
    }

    @Test
    void createOrder_shouldSetDefaultPaymentMethodToCOD() {
        OrderRequest request = createValidOrderRequest();
        request.setPaymentMethod(null);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-123");
            return order;
        });

        OrderResponse response = orderService.createOrder(request, "user-1", "John", "john@mail.com");

        assertThat(response.getPaymentMethod()).isEqualTo("COD");
    }

    @Test
    void createOrder_shouldCalculateTotalAmountCorrectly() {
        OrderRequest request = createValidOrderRequest();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-123");
            return order;
        });

        orderService.createOrder(request, "user-1", "John", "john@mail.com");

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getTotalAmount()).isEqualTo(150.0);
    }

    // ==================== GET ORDER BY ID TESTS ====================

    @Test
    void getOrderById_shouldReturnOrderForOwner() {
        Order order = createSampleOrder("order-123", "user-1");
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById("order-123", "user-1", false);

        assertThat(response.getId()).isEqualTo("order-123");
        assertThat(response.getUserId()).isEqualTo("user-1");
    }

    @Test
    void getOrderById_shouldReturnOrderForSellerWithProducts() {
        Order order = createSampleOrder("order-123", "user-1");
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById("order-123", "seller-1", true);

        assertThat(response.getId()).isEqualTo("order-123");
    }

    @Test
    void getOrderById_shouldThrowWhenOrderNotFound() {
        when(orderRepository.findById("order-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById("order-123", "user-1", false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void getOrderById_shouldThrowWhenUserNotAuthorized() {
        Order order = createSampleOrder("order-123", "user-1");
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById("order-123", "other-user", false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void getOrderById_shouldThrowWhenSellerHasNoProductsInOrder() {
        Order order = createSampleOrder("order-123", "user-1");
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById("order-123", "other-seller", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authorized");
    }

    // ==================== GET ORDERS BY USER TESTS ====================

    @Test
    void getOrdersByUser_shouldReturnUserOrders() {
        List<Order> orders = Arrays.asList(
                createSampleOrder("order-1", "user-1"),
                createSampleOrder("order-2", "user-1")
        );
        when(orderRepository.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(orders);

        List<OrderResponse> responses = orderService.getOrdersByUser("user-1");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo("order-1");
        assertThat(responses.get(1).getId()).isEqualTo("order-2");
    }

    @Test
    void getOrdersByUser_shouldReturnEmptyListWhenNoOrders() {
        when(orderRepository.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(Collections.emptyList());

        List<OrderResponse> responses = orderService.getOrdersByUser("user-1");

        assertThat(responses).isEmpty();
    }

    @Test
    void getOrdersByUserAndStatus_shouldReturnFilteredOrders() {
        Order order = createSampleOrder("order-1", "user-1");
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc("user-1", OrderStatus.SHIPPED))
                .thenReturn(Collections.singletonList(order));

        List<OrderResponse> responses = orderService.getOrdersByUserAndStatus("user-1", OrderStatus.SHIPPED);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    // ==================== GET ORDERS FOR SELLER TESTS ====================

    @Test
    void getOrdersForSeller_shouldReturnSellerOrders() {
        List<Order> orders = Arrays.asList(
                createSampleOrder("order-1", "user-1"),
                createSampleOrder("order-2", "user-2")
        );
        when(orderRepository.findBySellerId("seller-1")).thenReturn(orders);

        List<OrderResponse> responses = orderService.getOrdersForSeller("seller-1");

        assertThat(responses).hasSize(2);
    }

    @Test
    void getOrdersForSeller_shouldFilterItemsForSeller() {
        Order order = createSampleOrderWithMixedSellers("order-1", "user-1");

        when(orderRepository.findBySellerId("seller-1")).thenReturn(Collections.singletonList(order));

        List<OrderResponse> responses = orderService.getOrdersForSeller("seller-1");

        assertThat(responses).hasSize(1);
        // Should only contain seller-1's items
        assertThat(responses.get(0).getItems()).allMatch(item ->
            item.getSellerId().equals("seller-1"));
    }

    @Test
    void getOrdersForSellerByStatus_shouldReturnFilteredOrders() {
        Order order = createSampleOrder("order-1", "user-1");
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findBySellerIdAndStatus("seller-1", OrderStatus.CONFIRMED))
                .thenReturn(Collections.singletonList(order));

        List<OrderResponse> responses = orderService.getOrdersForSellerByStatus("seller-1", OrderStatus.CONFIRMED);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    // ==================== UPDATE ORDER STATUS TESTS ====================

    @Test
    void updateOrderStatus_shouldUpdateStatusSuccessfully() {
        Order order = createSampleOrder("order-123", "user-1");
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.CONFIRMED, null);
        OrderResponse response = orderService.updateOrderStatus("order-123", request, "seller-1");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(kafkaTemplate).send(eq("order-events"), eq("order-123"), any(OrderEvent.class));
    }

    @Test
    void updateOrderStatus_shouldSetConfirmedAtTimestamp() {
        Order order = createSampleOrder("order-123", "user-1");
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.CONFIRMED, null);
        orderService.updateOrderStatus("order-123", request, "seller-1");

        assertThat(orderCaptor.getValue().getConfirmedAt()).isNotNull();
    }

    @Test
    void updateOrderStatus_shouldSetShippedAtTimestamp() {
        Order order = createSampleOrder("order-123", "user-1");
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.SHIPPED, null);
        orderService.updateOrderStatus("order-123", request, "seller-1");

        assertThat(orderCaptor.getValue().getShippedAt()).isNotNull();
    }

    @Test
    void updateOrderStatus_shouldSetDeliveredAtTimestamp() {
        Order order = createSampleOrder("order-123", "user-1");
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.DELIVERED, null);
        orderService.updateOrderStatus("order-123", request, "seller-1");

        assertThat(orderCaptor.getValue().getDeliveredAt()).isNotNull();
    }

    @Test
    void updateOrderStatus_shouldThrowWhenOrderNotFound() {
        when(orderRepository.findById("order-123")).thenReturn(Optional.empty());

        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.CONFIRMED, null);

        assertThatThrownBy(() -> orderService.updateOrderStatus("order-123", request, "seller-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void updateOrderStatus_shouldThrowWhenSellerNotAuthorized() {
        Order order = createSampleOrder("order-123", "user-1");
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.CONFIRMED, null);

        assertThatThrownBy(() -> orderService.updateOrderStatus("order-123", request, "other-seller"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void updateOrderStatus_shouldThrowOnInvalidTransitionFromPendingToShipped() {
        Order order = createSampleOrder("order-123", "user-1");
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.SHIPPED, null);

        assertThatThrownBy(() -> orderService.updateOrderStatus("order-123", request, "seller-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateOrderStatus_shouldThrowOnInvalidTransitionFromDeliveredToPending() {
        Order order = createSampleOrder("order-123", "user-1");
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.PENDING, null);

        assertThatThrownBy(() -> orderService.updateOrderStatus("order-123", request, "seller-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateOrderStatus_shouldThrowOnInvalidTransitionFromCancelled() {
        Order order = createSampleOrder("order-123", "user-1");
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.CONFIRMED, null);

        assertThatThrownBy(() -> orderService.updateOrderStatus("order-123", request, "seller-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid status transition");
    }

    // ==================== CANCEL ORDER TESTS ====================

    @Test
    void cancelOrder_shouldCancelPendingOrder() {
        Order order = createSampleOrder("order-123", "user-1");
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder("order-123", "user-1", "Changed my mind");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.getCancellationReason()).isEqualTo("Changed my mind");
        verify(kafkaTemplate).send(eq("order-events"), eq("order-123"), any(OrderEvent.class));
    }

    @Test
    void cancelOrder_shouldCancelConfirmedOrder() {
        Order order = createSampleOrder("order-123", "user-1");
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder("order-123", "user-1", null);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.getCancellationReason()).isEqualTo("Cancelled by customer");
    }

    @Test
    void cancelOrder_shouldSetCancelledAtTimestamp() {
        Order order = createSampleOrder("order-123", "user-1");
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.cancelOrder("order-123", "user-1", "reason");

        assertThat(orderCaptor.getValue().getCancelledAt()).isNotNull();
    }

    @Test
    void cancelOrder_shouldThrowWhenOrderNotFound() {
        when(orderRepository.findById("order-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder("order-123", "user-1", "reason"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void cancelOrder_shouldThrowWhenUserNotAuthorized() {
        Order order = createSampleOrder("order-123", "user-1");
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("order-123", "other-user", "reason"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void cancelOrder_shouldThrowWhenOrderAlreadyShipped() {
        Order order = createSampleOrder("order-123", "user-1");
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("order-123", "user-1", "reason"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cannot be cancelled");
    }

    @Test
    void cancelOrder_shouldThrowWhenOrderAlreadyDelivered() {
        Order order = createSampleOrder("order-123", "user-1");
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("order-123", "user-1", "reason"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cannot be cancelled");
    }

    // ==================== REORDER TESTS ====================

    @Test
    void reorder_shouldCreateNewOrderFromExisting() {
        Order originalOrder = createSampleOrder("order-123", "user-1");
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(originalOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-456");
            return order;
        });

        OrderResponse response = orderService.reorder("order-123", "user-1", "John", "john@mail.com");

        assertThat(response.getId()).isEqualTo("order-456");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getItems()).hasSize(originalOrder.getItems().size());
        assertThat(response.getShippingAddress()).isEqualTo(originalOrder.getShippingAddress());
    }

    @Test
    void reorder_shouldThrowWhenOrderNotFound() {
        when(orderRepository.findById("order-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.reorder("order-123", "user-1", "John", "john@mail.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void reorder_shouldThrowWhenUserNotAuthorized() {
        Order order = createSampleOrder("order-123", "user-1");
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.reorder("order-123", "other-user", "Other", "other@mail.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authorized");
    }

    // ==================== USER STATS TESTS ====================

    @Test
    void getUserStats_shouldCalculateStatsCorrectly() {
        Order order1 = createSampleOrder("order-1", "user-1");
        order1.setStatus(OrderStatus.DELIVERED);
        order1.setTotalAmount(100.0);

        Order order2 = createSampleOrder("order-2", "user-1");
        order2.setStatus(OrderStatus.DELIVERED);
        order2.setTotalAmount(200.0);

        Order order3 = createSampleOrder("order-3", "user-1");
        order3.setStatus(OrderStatus.PENDING);
        order3.setTotalAmount(50.0);

        when(orderRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(Arrays.asList(order1, order2, order3));

        OrderService.UserOrderStats stats = orderService.getUserStats("user-1");

        assertThat(stats.totalOrders()).isEqualTo(3);
        assertThat(stats.completedOrders()).isEqualTo(2);
        assertThat(stats.totalSpent()).isEqualTo(300.0); // Only delivered orders
    }

    @Test
    void getUserStats_shouldReturnZeroWhenNoOrders() {
        when(orderRepository.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(Collections.emptyList());

        OrderService.UserOrderStats stats = orderService.getUserStats("user-1");

        assertThat(stats.totalOrders()).isEqualTo(0);
        assertThat(stats.completedOrders()).isEqualTo(0);
        assertThat(stats.totalSpent()).isEqualTo(0.0);
    }

    // ==================== SELLER STATS TESTS ====================

    @Test
    void getSellerStats_shouldCalculateStatsCorrectly() {
        Order order1 = createSampleOrder("order-1", "user-1");
        order1.setStatus(OrderStatus.DELIVERED);

        Order order2 = createSampleOrder("order-2", "user-2");
        order2.setStatus(OrderStatus.DELIVERED);

        Order order3 = createSampleOrder("order-3", "user-3");
        order3.setStatus(OrderStatus.PENDING);

        when(orderRepository.findBySellerId("seller-1")).thenReturn(Arrays.asList(order1, order2, order3));

        OrderService.SellerOrderStats stats = orderService.getSellerStats("seller-1");

        assertThat(stats.totalOrders()).isEqualTo(3);
        assertThat(stats.completedOrders()).isEqualTo(2);
        // Each order has 2 items from seller-1: 50*2 + 25*2 = 150 per order, 2 delivered = 300
        assertThat(stats.totalRevenue()).isEqualTo(300.0);
        assertThat(stats.totalItemsSold()).isEqualTo(8); // 4 items per order * 2 delivered orders
    }

    @Test
    void getSellerStats_shouldReturnZeroWhenNoOrders() {
        when(orderRepository.findBySellerId("seller-1")).thenReturn(Collections.emptyList());

        OrderService.SellerOrderStats stats = orderService.getSellerStats("seller-1");

        assertThat(stats.totalOrders()).isEqualTo(0);
        assertThat(stats.completedOrders()).isEqualTo(0);
        assertThat(stats.totalRevenue()).isEqualTo(0.0);
        assertThat(stats.totalItemsSold()).isEqualTo(0);
    }

    @Test
    void getSellerStats_shouldOnlyCountSellerItems() {
        Order order = createSampleOrderWithMixedSellers("order-1", "user-1");
        order.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findBySellerId("seller-1")).thenReturn(Collections.singletonList(order));

        OrderService.SellerOrderStats stats = orderService.getSellerStats("seller-1");

        // Should only count seller-1's items, not seller-2's
        assertThat(stats.totalRevenue()).isEqualTo(150.0); // Only seller-1 items
        assertThat(stats.totalItemsSold()).isEqualTo(4); // Only seller-1 items
    }

    // ==================== HELPER METHODS ====================

    private OrderRequest createValidOrderRequest() {
        OrderItemRequest item1 = new OrderItemRequest("prod-1", "Product 1", "seller-1", "Seller One", 50.0, 2, null);
        OrderItemRequest item2 = new OrderItemRequest("prod-2", "Product 2", "seller-1", "Seller One", 25.0, 2, null);

        OrderRequest request = new OrderRequest();
        request.setItems(Arrays.asList(item1, item2));
        request.setShippingAddress("123 Main St");
        request.setShippingCity("Paris");
        request.setShippingPostalCode("75001");
        request.setShippingCountry("France");
        request.setPhoneNumber("+33123456789");
        request.setPaymentMethod("COD");
        request.setNotes("Test order");

        return request;
    }

    private Order createSampleOrder(String orderId, String userId) {
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setUserName("Test User");
        order.setUserEmail("test@mail.com");
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress("123 Main St");
        order.setShippingCity("Paris");
        order.setShippingPostalCode("75001");
        order.setShippingCountry("France");
        order.setPhoneNumber("+33123456789");
        order.setPaymentMethod("COD");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        OrderItem item1 = new OrderItem("prod-1", "Product 1", "seller-1", "Seller One", 50.0, 2, null);
        OrderItem item2 = new OrderItem("prod-2", "Product 2", "seller-1", "Seller One", 25.0, 2, null);
        order.setItems(Arrays.asList(item1, item2));
        order.calculateTotalAmount();

        return order;
    }

    private Order createSampleOrderWithMixedSellers(String orderId, String userId) {
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setUserName("Test User");
        order.setUserEmail("test@mail.com");
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress("123 Main St");
        order.setShippingCity("Paris");
        order.setShippingPostalCode("75001");
        order.setShippingCountry("France");
        order.setPhoneNumber("+33123456789");
        order.setPaymentMethod("COD");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Use ArrayList to allow modification
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("prod-1", "Product 1", "seller-1", "Seller One", 50.0, 2, null));
        items.add(new OrderItem("prod-2", "Product 2", "seller-1", "Seller One", 25.0, 2, null));
        items.add(new OrderItem("prod-3", "Other Product", "seller-2", "Other Seller", 500.0, 10, null));
        order.setItems(items);
        order.calculateTotalAmount();

        return order;
    }
}
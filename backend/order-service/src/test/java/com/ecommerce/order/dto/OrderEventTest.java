package com.ecommerce.order.dto;

import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderEventTest {

    @Test
    void shouldCreateWithNoArgsConstructor() {
        OrderEvent event = new OrderEvent();

        assertThat(event.getType()).isNull();
        assertThat(event.getOrderId()).isNull();
        assertThat(event.getUserId()).isNull();
        assertThat(event.getItems()).isNull();
        assertThat(event.getStatus()).isNull();
        assertThat(event.getCancellationReason()).isNull();
    }

    @Test
    void shouldCreateWithAllArgsConstructor() {
        List<OrderItem> items = createSampleItems();

        OrderEvent event = new OrderEvent(
                OrderEvent.EventType.ORDER_CREATED,
                "order-123",
                "user-1",
                items,
                OrderStatus.PENDING,
                null
        );

        assertThat(event.getType()).isEqualTo(OrderEvent.EventType.ORDER_CREATED);
        assertThat(event.getOrderId()).isEqualTo("order-123");
        assertThat(event.getUserId()).isEqualTo("user-1");
        assertThat(event.getItems()).hasSize(2);
        assertThat(event.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(event.getCancellationReason()).isNull();
    }

    @Test
    void shouldSetAndGetType() {
        OrderEvent event = new OrderEvent();
        event.setType(OrderEvent.EventType.ORDER_CONFIRMED);

        assertThat(event.getType()).isEqualTo(OrderEvent.EventType.ORDER_CONFIRMED);
    }

    @Test
    void shouldSetAndGetOrderId() {
        OrderEvent event = new OrderEvent();
        event.setOrderId("order-456");

        assertThat(event.getOrderId()).isEqualTo("order-456");
    }

    @Test
    void shouldSetAndGetUserId() {
        OrderEvent event = new OrderEvent();
        event.setUserId("user-2");

        assertThat(event.getUserId()).isEqualTo("user-2");
    }

    @Test
    void shouldSetAndGetItems() {
        OrderEvent event = new OrderEvent();
        List<OrderItem> items = createSampleItems();
        event.setItems(items);

        assertThat(event.getItems()).hasSize(2);
        assertThat(event.getItems().get(0).getProductId()).isEqualTo("prod-1");
    }

    @Test
    void shouldSetAndGetStatus() {
        OrderEvent event = new OrderEvent();
        event.setStatus(OrderStatus.SHIPPED);

        assertThat(event.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void shouldSetAndGetCancellationReason() {
        OrderEvent event = new OrderEvent();
        event.setCancellationReason("Customer requested cancellation");

        assertThat(event.getCancellationReason()).isEqualTo("Customer requested cancellation");
    }

    @Test
    void shouldHaveAllEventTypes() {
        assertThat(OrderEvent.EventType.values()).containsExactlyInAnyOrder(
                OrderEvent.EventType.ORDER_CREATED,
                OrderEvent.EventType.ORDER_CONFIRMED,
                OrderEvent.EventType.ORDER_SHIPPED,
                OrderEvent.EventType.ORDER_DELIVERED,
                OrderEvent.EventType.ORDER_CANCELLED
        );
    }

    @Test
    void shouldCreateOrderCreatedEvent() {
        OrderEvent event = createEvent(OrderEvent.EventType.ORDER_CREATED, OrderStatus.PENDING);

        assertThat(event.getType()).isEqualTo(OrderEvent.EventType.ORDER_CREATED);
        assertThat(event.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shouldCreateOrderConfirmedEvent() {
        OrderEvent event = createEvent(OrderEvent.EventType.ORDER_CONFIRMED, OrderStatus.CONFIRMED);

        assertThat(event.getType()).isEqualTo(OrderEvent.EventType.ORDER_CONFIRMED);
        assertThat(event.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void shouldCreateOrderShippedEvent() {
        OrderEvent event = createEvent(OrderEvent.EventType.ORDER_SHIPPED, OrderStatus.SHIPPED);

        assertThat(event.getType()).isEqualTo(OrderEvent.EventType.ORDER_SHIPPED);
        assertThat(event.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void shouldCreateOrderDeliveredEvent() {
        OrderEvent event = createEvent(OrderEvent.EventType.ORDER_DELIVERED, OrderStatus.DELIVERED);

        assertThat(event.getType()).isEqualTo(OrderEvent.EventType.ORDER_DELIVERED);
        assertThat(event.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void shouldCreateOrderCancelledEventWithReason() {
        OrderEvent event = new OrderEvent(
                OrderEvent.EventType.ORDER_CANCELLED,
                "order-123",
                "user-1",
                createSampleItems(),
                OrderStatus.CANCELLED,
                "Out of stock"
        );

        assertThat(event.getType()).isEqualTo(OrderEvent.EventType.ORDER_CANCELLED);
        assertThat(event.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(event.getCancellationReason()).isEqualTo("Out of stock");
    }

    private List<OrderItem> createSampleItems() {
        return Arrays.asList(
                new OrderItem("prod-1", "Product 1", "seller-1", "Seller", 50.0, 2, null),
                new OrderItem("prod-2", "Product 2", "seller-1", "Seller", 25.0, 3, null)
        );
    }

    private OrderEvent createEvent(OrderEvent.EventType type, OrderStatus status) {
        return new OrderEvent(
                type,
                "order-123",
                "user-1",
                createSampleItems(),
                status,
                null
        );
    }
}
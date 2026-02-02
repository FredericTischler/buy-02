package com.ecommerce.order.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void testCalculateTotalAmount() {
        Order order = new Order();

        OrderItem item1 = new OrderItem();
        item1.setPrice(100.0);
        item1.setQuantity(2);

        OrderItem item2 = new OrderItem();
        item2.setPrice(50.0);
        item2.setQuantity(3);

        order.setItems(Arrays.asList(item1, item2));
        order.calculateTotalAmount();

        assertThat(order.getTotalAmount()).isEqualTo(350.0); // 100*2 + 50*3 = 200 + 150 = 350
    }

    @Test
    void testCalculateTotalAmountWithSingleItem() {
        Order order = new Order();

        OrderItem item = new OrderItem();
        item.setPrice(75.0);
        item.setQuantity(1);

        order.setItems(Collections.singletonList(item));
        order.calculateTotalAmount();

        assertThat(order.getTotalAmount()).isEqualTo(75.0);
    }

    @Test
    void testCalculateTotalAmountWithEmptyItems() {
        Order order = new Order();
        order.setItems(new ArrayList<>());
        order.calculateTotalAmount();

        assertThat(order.getTotalAmount()).isEqualTo(0.0);
    }

    @Test
    void testCalculateTotalAmountWithDecimalPrices() {
        Order order = new Order();

        OrderItem item1 = new OrderItem();
        item1.setPrice(19.99);
        item1.setQuantity(2);

        OrderItem item2 = new OrderItem();
        item2.setPrice(9.99);
        item2.setQuantity(1);

        order.setItems(Arrays.asList(item1, item2));
        order.calculateTotalAmount();

        assertThat(order.getTotalAmount()).isEqualTo(49.97); // 19.99*2 + 9.99 = 39.98 + 9.99
    }

    @Test
    void testDefaultOrderStatus() {
        Order order = new Order();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void testDefaultPaymentMethod() {
        Order order = new Order();
        assertThat(order.getPaymentMethod()).isEqualTo("COD");
    }

    @Test
    void testDefaultItemsList() {
        Order order = new Order();
        assertThat(order.getItems()).isNotNull();
        assertThat(order.getItems()).isEmpty();
    }

    @Test
    void testSetAndGetId() {
        Order order = new Order();
        order.setId("order-123");

        assertThat(order.getId()).isEqualTo("order-123");
    }

    @Test
    void testSetAndGetUserId() {
        Order order = new Order();
        order.setUserId("user-456");

        assertThat(order.getUserId()).isEqualTo("user-456");
    }

    @Test
    void testSetAndGetUserName() {
        Order order = new Order();
        order.setUserName("John Doe");

        assertThat(order.getUserName()).isEqualTo("John Doe");
    }

    @Test
    void testSetAndGetUserEmail() {
        Order order = new Order();
        order.setUserEmail("john@example.com");

        assertThat(order.getUserEmail()).isEqualTo("john@example.com");
    }

    @Test
    void testSetAndGetShippingAddress() {
        Order order = new Order();
        order.setShippingAddress("123 Main St");

        assertThat(order.getShippingAddress()).isEqualTo("123 Main St");
    }

    @Test
    void testSetAndGetShippingCity() {
        Order order = new Order();
        order.setShippingCity("Paris");

        assertThat(order.getShippingCity()).isEqualTo("Paris");
    }

    @Test
    void testSetAndGetShippingPostalCode() {
        Order order = new Order();
        order.setShippingPostalCode("75001");

        assertThat(order.getShippingPostalCode()).isEqualTo("75001");
    }

    @Test
    void testSetAndGetShippingCountry() {
        Order order = new Order();
        order.setShippingCountry("France");

        assertThat(order.getShippingCountry()).isEqualTo("France");
    }

    @Test
    void testSetAndGetPhoneNumber() {
        Order order = new Order();
        order.setPhoneNumber("+33123456789");

        assertThat(order.getPhoneNumber()).isEqualTo("+33123456789");
    }

    @Test
    void testSetAndGetNotes() {
        Order order = new Order();
        order.setNotes("Please deliver in the morning");

        assertThat(order.getNotes()).isEqualTo("Please deliver in the morning");
    }

    @Test
    void testSetAndGetCreatedAt() {
        Order order = new Order();
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);

        assertThat(order.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void testSetAndGetUpdatedAt() {
        Order order = new Order();
        LocalDateTime now = LocalDateTime.now();
        order.setUpdatedAt(now);

        assertThat(order.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void testSetAndGetConfirmedAt() {
        Order order = new Order();
        LocalDateTime now = LocalDateTime.now();
        order.setConfirmedAt(now);

        assertThat(order.getConfirmedAt()).isEqualTo(now);
    }

    @Test
    void testSetAndGetShippedAt() {
        Order order = new Order();
        LocalDateTime now = LocalDateTime.now();
        order.setShippedAt(now);

        assertThat(order.getShippedAt()).isEqualTo(now);
    }

    @Test
    void testSetAndGetDeliveredAt() {
        Order order = new Order();
        LocalDateTime now = LocalDateTime.now();
        order.setDeliveredAt(now);

        assertThat(order.getDeliveredAt()).isEqualTo(now);
    }

    @Test
    void testSetAndGetCancelledAt() {
        Order order = new Order();
        LocalDateTime now = LocalDateTime.now();
        order.setCancelledAt(now);

        assertThat(order.getCancelledAt()).isEqualTo(now);
    }

    @Test
    void testSetAndGetCancellationReason() {
        Order order = new Order();
        order.setCancellationReason("Customer requested cancellation");

        assertThat(order.getCancellationReason()).isEqualTo("Customer requested cancellation");
    }

    @Test
    void testSetAndGetStatus() {
        Order order = new Order();
        order.setStatus(OrderStatus.SHIPPED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void testAllOrderStatuses() {
        Order order = new Order();

        for (OrderStatus status : OrderStatus.values()) {
            order.setStatus(status);
            assertThat(order.getStatus()).isEqualTo(status);
        }
    }

    @Test
    void testOrderWithMultipleSellers() {
        Order order = new Order();

        OrderItem item1 = new OrderItem("prod-1", "Product 1", "seller-1", "Seller One", 50.0, 2, null);
        OrderItem item2 = new OrderItem("prod-2", "Product 2", "seller-2", "Seller Two", 30.0, 3, null);

        order.setItems(Arrays.asList(item1, item2));
        order.calculateTotalAmount();

        assertThat(order.getTotalAmount()).isEqualTo(190.0); // 50*2 + 30*3 = 100 + 90
        assertThat(order.getItems()).hasSize(2);
    }
}
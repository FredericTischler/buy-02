package com.ecommerce.order.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

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

        assertEquals(350.0, order.getTotalAmount()); // 100*2 + 50*3 = 200 + 150 = 350
    }

    @Test
    void testOrderItemSubtotal() {
        OrderItem item = new OrderItem();
        item.setPrice(25.0);
        item.setQuantity(4);

        assertEquals(100.0, item.getSubtotal());
    }

    @Test
    void testDefaultOrderStatus() {
        Order order = new Order();
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void testDefaultPaymentMethod() {
        Order order = new Order();
        assertEquals("COD", order.getPaymentMethod());
    }
}
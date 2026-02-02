package com.ecommerce.order.dto;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class OrderResponseTest {

    @Test
    void testFromOrder() {
        Order order = new Order();
        order.setId("order123");
        order.setUserId("user456");
        order.setUserName("John Doe");
        order.setUserEmail("john@example.com");
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(250.0);
        order.setShippingAddress("123 Main St");
        order.setShippingCity("Paris");
        order.setShippingPostalCode("75001");
        order.setShippingCountry("France");
        order.setPhoneNumber("+33123456789");
        order.setPaymentMethod("COD");
        order.setCreatedAt(LocalDateTime.now());

        OrderItem item = new OrderItem();
        item.setProductId("prod1");
        item.setProductName("Test Product");
        item.setPrice(50.0);
        item.setQuantity(5);
        order.setItems(Arrays.asList(item));

        OrderResponse response = OrderResponse.fromOrder(order);

        assertEquals("order123", response.getId());
        assertEquals("user456", response.getUserId());
        assertEquals("John Doe", response.getUserName());
        assertEquals("john@example.com", response.getUserEmail());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(250.0, response.getTotalAmount());
        assertEquals("Paris", response.getShippingCity());
        assertEquals(1, response.getItems().size());
    }
}
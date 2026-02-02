package com.ecommerce.order.dto;

import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    public enum EventType {
        ORDER_CREATED,
        ORDER_CONFIRMED,
        ORDER_SHIPPED,
        ORDER_DELIVERED,
        ORDER_CANCELLED
    }

    private EventType type;
    private String orderId;
    private String userId;
    private List<OrderItem> items;
    private OrderStatus status;
    private String cancellationReason;
}
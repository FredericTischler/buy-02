package com.ecommerce.order.model;

public enum OrderStatus {
    PENDING,      // Order created, awaiting confirmation
    CONFIRMED,    // Order confirmed by seller
    PROCESSING,   // Order being prepared
    SHIPPED,      // Order shipped
    DELIVERED,    // Order delivered to customer
    CANCELLED,    // Order cancelled
    REFUNDED      // Order refunded
}
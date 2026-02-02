package com.ecommerce.order.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String userId;
    private String userName;
    private String userEmail;

    private List<OrderItem> items = new ArrayList<>();

    private Double totalAmount;

    private OrderStatus status = OrderStatus.PENDING;

    private String shippingAddress;
    private String shippingCity;
    private String shippingPostalCode;
    private String shippingCountry;
    private String phoneNumber;

    private String paymentMethod = "COD"; // Cash on Delivery by default

    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;

    private String cancellationReason;

    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .mapToDouble(OrderItem::getSubtotal)
                .sum();
    }
}
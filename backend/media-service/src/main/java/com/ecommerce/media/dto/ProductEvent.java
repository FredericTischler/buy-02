package com.ecommerce.media.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent {
    private String eventType; // "CREATED", "UPDATED", "DELETED"
    private String productId;
    private String productName;
    private String sellerId;
    private LocalDateTime timestamp;
}

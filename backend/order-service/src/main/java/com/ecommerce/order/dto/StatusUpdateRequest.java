package com.ecommerce.order.dto;

import com.ecommerce.order.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    private String reason; // For cancellation reason
}
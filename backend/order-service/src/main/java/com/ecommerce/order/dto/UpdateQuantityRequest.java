package com.ecommerce.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuantityRequest {
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be 0 or positive")
    private Integer quantity;
}
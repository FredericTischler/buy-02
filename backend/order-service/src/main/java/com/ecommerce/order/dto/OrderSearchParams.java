package com.ecommerce.order.dto;

import com.ecommerce.order.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSearchParams {

    private String keyword;
    private OrderStatus status;
    private String sortBy = "createdAt"; // createdAt, totalAmount, status
    private String sortDir = "desc"; // asc, desc

    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }

    public boolean hasStatus() {
        return status != null;
    }

    public boolean isAscending() {
        return "asc".equalsIgnoreCase(sortDir);
    }
}
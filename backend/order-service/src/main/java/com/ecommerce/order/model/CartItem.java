package com.ecommerce.order.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private String productId;
    private String productName;
    private String sellerId;
    private String sellerName;
    private Double price;
    private Integer quantity;
    private Integer stock;
    private String imageUrl;

    public Double getSubtotal() {
        return price * quantity;
    }
}
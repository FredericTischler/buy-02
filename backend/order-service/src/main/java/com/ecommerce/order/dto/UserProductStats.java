package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProductStats {

    private List<ProductPurchaseInfo> mostPurchasedProducts;
    private List<CategoryStats> topCategories;
    private int totalUniqueProducts;
    private int totalItemsPurchased;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPurchaseInfo {
        private String productId;
        private String productName;
        private String sellerId;
        private String sellerName;
        private int totalQuantity;
        private double totalSpent;
        private LocalDateTime lastPurchased;
        private String imageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStats {
        private String category;
        private int orderCount;
        private int itemCount;
        private double totalSpent;
    }
}
package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerProductStats {

    private List<BestSellingProduct> bestSellingProducts;
    private List<RecentSale> recentSales;
    private int totalUniqueProductsSold;
    private int totalCustomers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BestSellingProduct {
        private String productId;
        private String productName;
        private int totalSold;
        private double revenue;
        private int orderCount;
        private String imageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentSale {
        private String orderId;
        private String productId;
        private String productName;
        private String customerName;
        private int quantity;
        private double amount;
        private LocalDateTime saleDate;
    }
}
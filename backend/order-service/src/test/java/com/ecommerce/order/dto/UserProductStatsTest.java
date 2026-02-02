package com.ecommerce.order.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserProductStatsTest {

    @Test
    void shouldCreateWithNoArgsConstructor() {
        UserProductStats stats = new UserProductStats();

        assertThat(stats.getMostPurchasedProducts()).isNull();
        assertThat(stats.getTopCategories()).isNull();
        assertThat(stats.getTotalUniqueProducts()).isEqualTo(0);
        assertThat(stats.getTotalItemsPurchased()).isEqualTo(0);
    }

    @Test
    void shouldCreateWithAllArgsConstructor() {
        List<UserProductStats.ProductPurchaseInfo> products = Arrays.asList(
                createProductPurchaseInfo("prod-1", "Product 1", 10, 100.0)
        );
        List<UserProductStats.CategoryStats> categories = Arrays.asList(
                new UserProductStats.CategoryStats("Seller A", 5, 20, 500.0)
        );

        UserProductStats stats = new UserProductStats(products, categories, 5, 25);

        assertThat(stats.getMostPurchasedProducts()).hasSize(1);
        assertThat(stats.getTopCategories()).hasSize(1);
        assertThat(stats.getTotalUniqueProducts()).isEqualTo(5);
        assertThat(stats.getTotalItemsPurchased()).isEqualTo(25);
    }

    @Test
    void shouldSetAndGetMostPurchasedProducts() {
        UserProductStats stats = new UserProductStats();
        List<UserProductStats.ProductPurchaseInfo> products = Arrays.asList(
                createProductPurchaseInfo("prod-1", "Product 1", 5, 50.0),
                createProductPurchaseInfo("prod-2", "Product 2", 3, 30.0)
        );

        stats.setMostPurchasedProducts(products);

        assertThat(stats.getMostPurchasedProducts()).hasSize(2);
        assertThat(stats.getMostPurchasedProducts().get(0).getProductName()).isEqualTo("Product 1");
    }

    @Test
    void shouldSetAndGetTopCategories() {
        UserProductStats stats = new UserProductStats();
        List<UserProductStats.CategoryStats> categories = Arrays.asList(
                new UserProductStats.CategoryStats("Electronics", 10, 50, 1000.0)
        );

        stats.setTopCategories(categories);

        assertThat(stats.getTopCategories()).hasSize(1);
        assertThat(stats.getTopCategories().get(0).getCategory()).isEqualTo("Electronics");
    }

    @Test
    void shouldCreateProductPurchaseInfo() {
        LocalDateTime now = LocalDateTime.now();
        UserProductStats.ProductPurchaseInfo info = new UserProductStats.ProductPurchaseInfo(
                "prod-1", "Product 1", "seller-1", "Seller One", 10, 250.0, now, "/image.jpg"
        );

        assertThat(info.getProductId()).isEqualTo("prod-1");
        assertThat(info.getProductName()).isEqualTo("Product 1");
        assertThat(info.getSellerId()).isEqualTo("seller-1");
        assertThat(info.getSellerName()).isEqualTo("Seller One");
        assertThat(info.getTotalQuantity()).isEqualTo(10);
        assertThat(info.getTotalSpent()).isEqualTo(250.0);
        assertThat(info.getLastPurchased()).isEqualTo(now);
        assertThat(info.getImageUrl()).isEqualTo("/image.jpg");
    }

    @Test
    void shouldCreateCategoryStats() {
        UserProductStats.CategoryStats catStats = new UserProductStats.CategoryStats(
                "Electronics", 5, 15, 750.0
        );

        assertThat(catStats.getCategory()).isEqualTo("Electronics");
        assertThat(catStats.getOrderCount()).isEqualTo(5);
        assertThat(catStats.getItemCount()).isEqualTo(15);
        assertThat(catStats.getTotalSpent()).isEqualTo(750.0);
    }

    @Test
    void shouldModifyProductPurchaseInfo() {
        UserProductStats.ProductPurchaseInfo info = new UserProductStats.ProductPurchaseInfo();
        LocalDateTime now = LocalDateTime.now();

        info.setProductId("prod-1");
        info.setProductName("Updated Product");
        info.setSellerId("seller-1");
        info.setSellerName("Updated Seller");
        info.setTotalQuantity(20);
        info.setTotalSpent(500.0);
        info.setLastPurchased(now);
        info.setImageUrl("/new-image.jpg");

        assertThat(info.getProductId()).isEqualTo("prod-1");
        assertThat(info.getProductName()).isEqualTo("Updated Product");
        assertThat(info.getTotalQuantity()).isEqualTo(20);
        assertThat(info.getTotalSpent()).isEqualTo(500.0);
    }

    @Test
    void shouldModifyCategoryStats() {
        UserProductStats.CategoryStats catStats = new UserProductStats.CategoryStats();

        catStats.setCategory("Fashion");
        catStats.setOrderCount(10);
        catStats.setItemCount(30);
        catStats.setTotalSpent(1500.0);

        assertThat(catStats.getCategory()).isEqualTo("Fashion");
        assertThat(catStats.getOrderCount()).isEqualTo(10);
        assertThat(catStats.getItemCount()).isEqualTo(30);
        assertThat(catStats.getTotalSpent()).isEqualTo(1500.0);
    }

    private UserProductStats.ProductPurchaseInfo createProductPurchaseInfo(
            String productId, String productName, int quantity, double spent) {
        return new UserProductStats.ProductPurchaseInfo(
                productId, productName, "seller-1", "Seller", quantity, spent, LocalDateTime.now(), null
        );
    }
}
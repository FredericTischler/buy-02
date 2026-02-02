package com.ecommerce.order.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SellerProductStatsTest {

    @Test
    void shouldCreateWithNoArgsConstructor() {
        SellerProductStats stats = new SellerProductStats();

        assertThat(stats.getBestSellingProducts()).isNull();
        assertThat(stats.getRecentSales()).isNull();
        assertThat(stats.getTotalUniqueProductsSold()).isEqualTo(0);
        assertThat(stats.getTotalCustomers()).isEqualTo(0);
    }

    @Test
    void shouldCreateWithAllArgsConstructor() {
        List<SellerProductStats.BestSellingProduct> products = Arrays.asList(
                createBestSellingProduct("prod-1", "Product 1", 50, 500.0)
        );
        List<SellerProductStats.RecentSale> sales = Arrays.asList(
                createRecentSale("order-1", "prod-1", "Product 1", "John", 2, 50.0)
        );

        SellerProductStats stats = new SellerProductStats(products, sales, 10, 25);

        assertThat(stats.getBestSellingProducts()).hasSize(1);
        assertThat(stats.getRecentSales()).hasSize(1);
        assertThat(stats.getTotalUniqueProductsSold()).isEqualTo(10);
        assertThat(stats.getTotalCustomers()).isEqualTo(25);
    }

    @Test
    void shouldSetAndGetBestSellingProducts() {
        SellerProductStats stats = new SellerProductStats();
        List<SellerProductStats.BestSellingProduct> products = Arrays.asList(
                createBestSellingProduct("prod-1", "Product 1", 100, 1000.0),
                createBestSellingProduct("prod-2", "Product 2", 80, 800.0)
        );

        stats.setBestSellingProducts(products);

        assertThat(stats.getBestSellingProducts()).hasSize(2);
        assertThat(stats.getBestSellingProducts().get(0).getTotalSold()).isEqualTo(100);
    }

    @Test
    void shouldSetAndGetRecentSales() {
        SellerProductStats stats = new SellerProductStats();
        List<SellerProductStats.RecentSale> sales = Arrays.asList(
                createRecentSale("order-1", "prod-1", "Product 1", "Alice", 3, 75.0)
        );

        stats.setRecentSales(sales);

        assertThat(stats.getRecentSales()).hasSize(1);
        assertThat(stats.getRecentSales().get(0).getCustomerName()).isEqualTo("Alice");
    }

    @Test
    void shouldCreateBestSellingProduct() {
        SellerProductStats.BestSellingProduct product = new SellerProductStats.BestSellingProduct(
                "prod-1", "Product 1", 150, 3000.0, 45, "/image.jpg"
        );

        assertThat(product.getProductId()).isEqualTo("prod-1");
        assertThat(product.getProductName()).isEqualTo("Product 1");
        assertThat(product.getTotalSold()).isEqualTo(150);
        assertThat(product.getRevenue()).isEqualTo(3000.0);
        assertThat(product.getOrderCount()).isEqualTo(45);
        assertThat(product.getImageUrl()).isEqualTo("/image.jpg");
    }

    @Test
    void shouldCreateRecentSale() {
        LocalDateTime now = LocalDateTime.now();
        SellerProductStats.RecentSale sale = new SellerProductStats.RecentSale(
                "order-123", "prod-1", "Product 1", "Bob", 5, 125.0, now
        );

        assertThat(sale.getOrderId()).isEqualTo("order-123");
        assertThat(sale.getProductId()).isEqualTo("prod-1");
        assertThat(sale.getProductName()).isEqualTo("Product 1");
        assertThat(sale.getCustomerName()).isEqualTo("Bob");
        assertThat(sale.getQuantity()).isEqualTo(5);
        assertThat(sale.getAmount()).isEqualTo(125.0);
        assertThat(sale.getSaleDate()).isEqualTo(now);
    }

    @Test
    void shouldModifyBestSellingProduct() {
        SellerProductStats.BestSellingProduct product = new SellerProductStats.BestSellingProduct();

        product.setProductId("prod-2");
        product.setProductName("New Product");
        product.setTotalSold(200);
        product.setRevenue(4000.0);
        product.setOrderCount(60);
        product.setImageUrl("/new-image.jpg");

        assertThat(product.getProductId()).isEqualTo("prod-2");
        assertThat(product.getProductName()).isEqualTo("New Product");
        assertThat(product.getTotalSold()).isEqualTo(200);
        assertThat(product.getRevenue()).isEqualTo(4000.0);
        assertThat(product.getOrderCount()).isEqualTo(60);
    }

    @Test
    void shouldModifyRecentSale() {
        SellerProductStats.RecentSale sale = new SellerProductStats.RecentSale();
        LocalDateTime now = LocalDateTime.now();

        sale.setOrderId("order-456");
        sale.setProductId("prod-2");
        sale.setProductName("Another Product");
        sale.setCustomerName("Charlie");
        sale.setQuantity(10);
        sale.setAmount(250.0);
        sale.setSaleDate(now);

        assertThat(sale.getOrderId()).isEqualTo("order-456");
        assertThat(sale.getCustomerName()).isEqualTo("Charlie");
        assertThat(sale.getQuantity()).isEqualTo(10);
        assertThat(sale.getAmount()).isEqualTo(250.0);
    }

    @Test
    void shouldSetTotalUniqueProductsSold() {
        SellerProductStats stats = new SellerProductStats();
        stats.setTotalUniqueProductsSold(50);

        assertThat(stats.getTotalUniqueProductsSold()).isEqualTo(50);
    }

    @Test
    void shouldSetTotalCustomers() {
        SellerProductStats stats = new SellerProductStats();
        stats.setTotalCustomers(100);

        assertThat(stats.getTotalCustomers()).isEqualTo(100);
    }

    private SellerProductStats.BestSellingProduct createBestSellingProduct(
            String productId, String productName, int totalSold, double revenue) {
        return new SellerProductStats.BestSellingProduct(
                productId, productName, totalSold, revenue, 10, null
        );
    }

    private SellerProductStats.RecentSale createRecentSale(
            String orderId, String productId, String productName, String customer, int quantity, double amount) {
        return new SellerProductStats.RecentSale(
                orderId, productId, productName, customer, quantity, amount, LocalDateTime.now()
        );
    }
}
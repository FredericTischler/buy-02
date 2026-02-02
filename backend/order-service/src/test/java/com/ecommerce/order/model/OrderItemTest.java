package com.ecommerce.order.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemTest {

    @Test
    void shouldCreateWithNoArgsConstructor() {
        OrderItem item = new OrderItem();

        assertThat(item.getProductId()).isNull();
        assertThat(item.getProductName()).isNull();
        assertThat(item.getSellerId()).isNull();
        assertThat(item.getSellerName()).isNull();
        assertThat(item.getPrice()).isNull();
        assertThat(item.getQuantity()).isNull();
        assertThat(item.getImageUrl()).isNull();
    }

    @Test
    void shouldCreateWithAllArgsConstructor() {
        OrderItem item = new OrderItem(
                "prod-1", "Product 1", "seller-1", "Seller One", 50.0, 2, "/image.jpg"
        );

        assertThat(item.getProductId()).isEqualTo("prod-1");
        assertThat(item.getProductName()).isEqualTo("Product 1");
        assertThat(item.getSellerId()).isEqualTo("seller-1");
        assertThat(item.getSellerName()).isEqualTo("Seller One");
        assertThat(item.getPrice()).isEqualTo(50.0);
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getImageUrl()).isEqualTo("/image.jpg");
    }

    @Test
    void shouldCalculateSubtotal() {
        OrderItem item = new OrderItem();
        item.setPrice(25.0);
        item.setQuantity(4);

        assertThat(item.getSubtotal()).isEqualTo(100.0);
    }

    @Test
    void shouldCalculateSubtotalWithDecimalPrice() {
        OrderItem item = new OrderItem();
        item.setPrice(19.99);
        item.setQuantity(3);

        assertThat(item.getSubtotal()).isEqualTo(59.97);
    }

    @Test
    void shouldCalculateSubtotalForSingleItem() {
        OrderItem item = new OrderItem();
        item.setPrice(100.0);
        item.setQuantity(1);

        assertThat(item.getSubtotal()).isEqualTo(100.0);
    }

    @Test
    void shouldSetAndGetProductId() {
        OrderItem item = new OrderItem();
        item.setProductId("prod-123");

        assertThat(item.getProductId()).isEqualTo("prod-123");
    }

    @Test
    void shouldSetAndGetProductName() {
        OrderItem item = new OrderItem();
        item.setProductName("Test Product");

        assertThat(item.getProductName()).isEqualTo("Test Product");
    }

    @Test
    void shouldSetAndGetSellerId() {
        OrderItem item = new OrderItem();
        item.setSellerId("seller-123");

        assertThat(item.getSellerId()).isEqualTo("seller-123");
    }

    @Test
    void shouldSetAndGetSellerName() {
        OrderItem item = new OrderItem();
        item.setSellerName("Test Seller");

        assertThat(item.getSellerName()).isEqualTo("Test Seller");
    }

    @Test
    void shouldSetAndGetPrice() {
        OrderItem item = new OrderItem();
        item.setPrice(99.99);

        assertThat(item.getPrice()).isEqualTo(99.99);
    }

    @Test
    void shouldSetAndGetQuantity() {
        OrderItem item = new OrderItem();
        item.setQuantity(10);

        assertThat(item.getQuantity()).isEqualTo(10);
    }

    @Test
    void shouldSetAndGetImageUrl() {
        OrderItem item = new OrderItem();
        item.setImageUrl("/api/media/file/prod-1/image.png");

        assertThat(item.getImageUrl()).isEqualTo("/api/media/file/prod-1/image.png");
    }

    @Test
    void shouldHandleNullImageUrl() {
        OrderItem item = new OrderItem("prod-1", "Product", "seller-1", "Seller", 10.0, 1, null);

        assertThat(item.getImageUrl()).isNull();
    }
}
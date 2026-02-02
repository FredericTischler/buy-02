package com.ecommerce.product.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    void prePersistShouldInitializeTimestampsWhenMissing() {
        Product product = new Product();

        product.prePersist();

        assertThat(product.getCreatedAt())
            .isNotNull();
        assertThat(product.getUpdatedAt())
            .isNotNull()
            .isAfterOrEqualTo(product.getCreatedAt());
    }

    @Test
    void prePersistShouldNotOverrideExistingCreatedAt() {
        Product product = new Product();
        LocalDateTime created = LocalDateTime.now().minusDays(1);
        product.setCreatedAt(created);

        product.prePersist();

        assertThat(product.getCreatedAt())
            .isEqualTo(created);
        assertThat(product.getUpdatedAt())
            .isAfterOrEqualTo(created);
    }

    @Test
    void allArgsConstructorShouldPopulateFields() {
        LocalDateTime now = LocalDateTime.now();
        Product product = new Product("id", "Phone", "Desc", 1000.0, "Tech", 5,
                "seller-1", "Alice", now.minusDays(1), now);

        assertThat(product)
            .extracting(Product::getId, Product::getSellerName, Product::getUpdatedAt)
            .containsExactly("id", "Alice", now);
    }

    @Test
    void gettersAndSettersShouldWorkForAllFields() {
        Product product = new Product();
        product.setId("id");
        product.setName("Name");
        product.setDescription("Desc");
        product.setPrice(10.0);
        product.setCategory("Cat");
        product.setStock(2);
        product.setSellerId("seller");
        product.setSellerName("Seller Name");

        assertThat(product)
            .extracting(Product::getDescription, Product::getSellerName, Product::getPrice)
            .containsExactly("Desc", "Seller Name", 10.0);
    }

    @Test
    void equalsAndHashCodeShouldUseAllFields() {
        LocalDateTime now = LocalDateTime.now();
        Product first = new Product("id", "Phone", "Desc", 1000.0, "Tech", 5,
            "seller-1", "Alice", now.minusDays(1), now);
        Product second = new Product("id", "Phone", "Desc", 1000.0, "Tech", 5,
            "seller-1", "Alice", now.minusDays(1), now);

        assertThat(first)
            .isEqualTo(second)
            .hasSameHashCodeAs(second);
        assertThat(first.toString()).contains("Phone");

        second.setName("Other");
        assertThat(first).isNotEqualTo(second);
    }
}

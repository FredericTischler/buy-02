package com.ecommerce.product.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductEventTest {

    @Test
    void allArgsConstructorShouldExposeValues() {
        LocalDateTime now = LocalDateTime.now();
        ProductEvent event = new ProductEvent("CREATED", "product-1", "Phone", "seller-1", now);

        assertThat(event)
            .extracting(ProductEvent::getEventType, ProductEvent::getProductName, ProductEvent::getTimestamp)
            .containsExactly("CREATED", "Phone", now);
    }

    @Test
    void settersShouldWorkWithDefaultConstructor() {
        ProductEvent event = new ProductEvent();
        event.setEventType("DELETED");
        event.setProductId("product-2");
        event.setSellerId("seller-2");

        assertThat(event)
            .extracting(ProductEvent::getEventType, ProductEvent::getProductId, ProductEvent::getSellerId)
            .containsExactly("DELETED", "product-2", "seller-2");
    }

    @Test
    void equalsAndHashCodeShouldConsiderValues() {
        LocalDateTime now = LocalDateTime.now();
        ProductEvent first = new ProductEvent("CREATED", "product-1", "Phone", "seller-1", now);
        ProductEvent second = new ProductEvent("CREATED", "product-1", "Phone", "seller-1", now);

        assertThat(first)
            .isEqualTo(second)
            .hasSameHashCodeAs(second);
        assertThat(first.toString()).contains("Phone");

        second.setProductName("Tablet");
        assertThat(first).isNotEqualTo(second);
    }
}

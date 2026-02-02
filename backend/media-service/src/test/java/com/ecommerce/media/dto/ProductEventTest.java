package com.ecommerce.media.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductEventTest {

    @Test
    void allArgsConstructorShouldExposeValues() {
        LocalDateTime now = LocalDateTime.now();
        ProductEvent event = new ProductEvent("DELETED", "product-1", "Phone", "seller-1", now);

        assertThat(event)
            .extracting(ProductEvent::getEventType, ProductEvent::getProductName, ProductEvent::getTimestamp)
            .containsExactly("DELETED", "Phone", now);
    }

    @Test
    void settersShouldWorkWithNoArgsConstructor() {
        ProductEvent event = new ProductEvent();
        event.setEventType("CREATED");
        event.setProductId("product-2");
        event.setSellerId("seller-2");

        assertThat(event)
            .extracting(ProductEvent::getEventType, ProductEvent::getProductId, ProductEvent::getSellerId)
            .containsExactly("CREATED", "product-2", "seller-2");
    }

    @Test
    void equalsAndHashCodeShouldDependOnFields() {
        LocalDateTime now = LocalDateTime.now();
        ProductEvent first = new ProductEvent("UPDATED", "p1", "Phone", "seller", now);
        ProductEvent second = new ProductEvent("UPDATED", "p1", "Phone", "seller", now);

        assertThat(first)
            .isEqualTo(second)
            .hasSameHashCodeAs(second);
        assertThat(first.toString()).contains("Phone");

        second.setEventType("DELETED");
        assertThat(first).isNotEqualTo(second);
    }
}

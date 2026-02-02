package com.ecommerce.order.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void shouldHaveAllExpectedStatuses() {
        assertThat(OrderStatus.values()).containsExactlyInAnyOrder(
                OrderStatus.PENDING,
                OrderStatus.CONFIRMED,
                OrderStatus.PROCESSING,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED,
                OrderStatus.CANCELLED,
                OrderStatus.REFUNDED
        );
    }

    @Test
    void shouldHaveCorrectNumberOfStatuses() {
        assertThat(OrderStatus.values()).hasSize(7);
    }

    @Test
    void shouldParseFromString() {
        assertThat(OrderStatus.valueOf("PENDING")).isEqualTo(OrderStatus.PENDING);
        assertThat(OrderStatus.valueOf("CONFIRMED")).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(OrderStatus.valueOf("PROCESSING")).isEqualTo(OrderStatus.PROCESSING);
        assertThat(OrderStatus.valueOf("SHIPPED")).isEqualTo(OrderStatus.SHIPPED);
        assertThat(OrderStatus.valueOf("DELIVERED")).isEqualTo(OrderStatus.DELIVERED);
        assertThat(OrderStatus.valueOf("CANCELLED")).isEqualTo(OrderStatus.CANCELLED);
        assertThat(OrderStatus.valueOf("REFUNDED")).isEqualTo(OrderStatus.REFUNDED);
    }

    @Test
    void shouldConvertToString() {
        assertThat(OrderStatus.PENDING.name()).isEqualTo("PENDING");
        assertThat(OrderStatus.CONFIRMED.name()).isEqualTo("CONFIRMED");
        assertThat(OrderStatus.PROCESSING.name()).isEqualTo("PROCESSING");
        assertThat(OrderStatus.SHIPPED.name()).isEqualTo("SHIPPED");
        assertThat(OrderStatus.DELIVERED.name()).isEqualTo("DELIVERED");
        assertThat(OrderStatus.CANCELLED.name()).isEqualTo("CANCELLED");
        assertThat(OrderStatus.REFUNDED.name()).isEqualTo("REFUNDED");
    }

    @Test
    void shouldHaveCorrectOrdinalValues() {
        assertThat(OrderStatus.PENDING.ordinal()).isEqualTo(0);
        assertThat(OrderStatus.CONFIRMED.ordinal()).isEqualTo(1);
        assertThat(OrderStatus.PROCESSING.ordinal()).isEqualTo(2);
        assertThat(OrderStatus.SHIPPED.ordinal()).isEqualTo(3);
        assertThat(OrderStatus.DELIVERED.ordinal()).isEqualTo(4);
        assertThat(OrderStatus.CANCELLED.ordinal()).isEqualTo(5);
        assertThat(OrderStatus.REFUNDED.ordinal()).isEqualTo(6);
    }
}
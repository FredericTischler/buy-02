package com.ecommerce.order.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldCreateValidOrderItemRequest() {
        OrderItemRequest item = new OrderItemRequest(
                "prod-1", "Product 1", "seller-1", "Seller", 50.0, 2, null
        );

        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(item);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailWhenProductIdBlank() {
        OrderItemRequest item = new OrderItemRequest(
                "", "Product 1", "seller-1", "Seller", 50.0, 2, null
        );

        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(item);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Product ID");
    }

    @Test
    void shouldFailWhenProductIdNull() {
        OrderItemRequest item = new OrderItemRequest(
                null, "Product 1", "seller-1", "Seller", 50.0, 2, null
        );

        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(item);

        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldFailWhenProductNameBlank() {
        OrderItemRequest item = new OrderItemRequest(
                "prod-1", "", "seller-1", "Seller", 50.0, 2, null
        );

        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(item);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Product name");
    }

    @Test
    void shouldFailWhenSellerIdBlank() {
        OrderItemRequest item = new OrderItemRequest(
                "prod-1", "Product 1", "", "Seller", 50.0, 2, null
        );

        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(item);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Seller ID");
    }

    @Test
    void shouldAcceptOptionalSellerName() {
        OrderItemRequest item = new OrderItemRequest(
                "prod-1", "Product 1", "seller-1", null, 50.0, 2, null
        );

        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(item);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailWhenPriceNull() {
        OrderItemRequest item = new OrderItemRequest(
                "prod-1", "Product 1", "seller-1", "Seller", null, 2, null
        );

        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(item);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Price");
    }

    @Test
    void shouldFailWhenPriceNotPositive() {
        OrderItemRequest item = new OrderItemRequest(
                "prod-1", "Product 1", "seller-1", "Seller", 0.0, 2, null
        );

        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(item);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("positive");
    }

    @Test
    void shouldFailWhenPriceNegative() {
        OrderItemRequest item = new OrderItemRequest(
                "prod-1", "Product 1", "seller-1", "Seller", -10.0, 2, null
        );

        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(item);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("positive");
    }

    @Test
    void shouldFailWhenQuantityNull() {
        OrderItemRequest item = new OrderItemRequest(
                "prod-1", "Product 1", "seller-1", "Seller", 50.0, null, null
        );

        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(item);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Quantity");
    }

    @Test
    void shouldFailWhenQuantityZero() {
        OrderItemRequest item = new OrderItemRequest(
                "prod-1", "Product 1", "seller-1", "Seller", 50.0, 0, null
        );

        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(item);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("at least 1");
    }

    @Test
    void shouldFailWhenQuantityNegative() {
        OrderItemRequest item = new OrderItemRequest(
                "prod-1", "Product 1", "seller-1", "Seller", 50.0, -1, null
        );

        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(item);

        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldAcceptOptionalImageUrl() {
        OrderItemRequest item = new OrderItemRequest(
                "prod-1", "Product 1", "seller-1", "Seller", 50.0, 2, null
        );

        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(item);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldAcceptImageUrl() {
        OrderItemRequest item = new OrderItemRequest(
                "prod-1", "Product 1", "seller-1", "Seller", 50.0, 2, "/api/media/file/prod-1/image.jpg"
        );

        Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(item);

        assertThat(violations).isEmpty();
        assertThat(item.getImageUrl()).isEqualTo("/api/media/file/prod-1/image.jpg");
    }
}
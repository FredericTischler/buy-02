package com.ecommerce.order.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldCreateValidOrderRequest() {
        OrderRequest request = createValidOrderRequest();

        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailWhenItemsEmpty() {
        OrderRequest request = createValidOrderRequest();
        request.setItems(Collections.emptyList());

        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("at least one item");
    }

    @Test
    void shouldFailWhenItemsNull() {
        OrderRequest request = createValidOrderRequest();
        request.setItems(null);

        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldFailWhenShippingAddressBlank() {
        OrderRequest request = createValidOrderRequest();
        request.setShippingAddress("");

        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Shipping address");
    }

    @Test
    void shouldFailWhenShippingCityBlank() {
        OrderRequest request = createValidOrderRequest();
        request.setShippingCity("");

        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("City");
    }

    @Test
    void shouldFailWhenShippingPostalCodeBlank() {
        OrderRequest request = createValidOrderRequest();
        request.setShippingPostalCode("");

        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Postal code");
    }

    @Test
    void shouldFailWhenShippingCountryBlank() {
        OrderRequest request = createValidOrderRequest();
        request.setShippingCountry("");

        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Country");
    }

    @Test
    void shouldFailWhenPhoneNumberBlank() {
        OrderRequest request = createValidOrderRequest();
        request.setPhoneNumber("");

        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Phone number");
    }

    @Test
    void shouldHaveDefaultPaymentMethodCOD() {
        OrderRequest request = new OrderRequest();

        assertThat(request.getPaymentMethod()).isEqualTo("COD");
    }

    @Test
    void shouldAcceptOptionalNotes() {
        OrderRequest request = createValidOrderRequest();
        request.setNotes(null);

        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldCascadeValidationToItems() {
        OrderRequest request = createValidOrderRequest();
        OrderItemRequest invalidItem = new OrderItemRequest();
        invalidItem.setProductId(""); // Invalid - blank
        request.setItems(Collections.singletonList(invalidItem));

        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    private OrderRequest createValidOrderRequest() {
        OrderItemRequest item = new OrderItemRequest(
                "prod-1", "Product 1", "seller-1", "Seller", 50.0, 2, null
        );

        OrderRequest request = new OrderRequest();
        request.setItems(Arrays.asList(item));
        request.setShippingAddress("123 Main St");
        request.setShippingCity("Paris");
        request.setShippingPostalCode("75001");
        request.setShippingCountry("France");
        request.setPhoneNumber("+33123456789");
        request.setPaymentMethod("COD");
        return request;
    }
}
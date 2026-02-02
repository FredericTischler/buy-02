package com.ecommerce.order.dto;

import com.ecommerce.order.model.OrderStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StatusUpdateRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldCreateValidStatusUpdateRequest() {
        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.CONFIRMED, null);

        Set<ConstraintViolation<StatusUpdateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldCreateWithReason() {
        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.CANCELLED, "Customer requested cancellation");

        Set<ConstraintViolation<StatusUpdateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
        assertThat(request.getReason()).isEqualTo("Customer requested cancellation");
    }

    @Test
    void shouldFailWhenStatusNull() {
        StatusUpdateRequest request = new StatusUpdateRequest(null, null);

        Set<ConstraintViolation<StatusUpdateRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Status");
    }

    @Test
    void shouldAcceptOptionalReason() {
        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.SHIPPED, null);

        Set<ConstraintViolation<StatusUpdateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
        assertThat(request.getReason()).isNull();
    }

    @Test
    void shouldGetAndSetStatus() {
        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(OrderStatus.DELIVERED);

        assertThat(request.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void shouldGetAndSetReason() {
        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setReason("Test reason");

        assertThat(request.getReason()).isEqualTo("Test reason");
    }

    @Test
    void shouldCreateWithAllArgsConstructor() {
        StatusUpdateRequest request = new StatusUpdateRequest(OrderStatus.PROCESSING, "Being prepared");

        assertThat(request.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(request.getReason()).isEqualTo("Being prepared");
    }

    @Test
    void shouldCreateWithNoArgsConstructor() {
        StatusUpdateRequest request = new StatusUpdateRequest();

        assertThat(request.getStatus()).isNull();
        assertThat(request.getReason()).isNull();
    }
}
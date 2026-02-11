package com.ecommerce.media.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleMaxUploadSize_shouldReturnPayloadTooLarge() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(2048L);

        ResponseEntity<Map<String, Object>> response = handler.handleMaxUploadSize(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).containsEntry("status", 413);
        assertThat(response.getBody()).containsEntry("error", "File too large");
        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    void handleRuntimeException_shouldReturnNotFoundForNotFoundMessage() {
        RuntimeException ex = new RuntimeException("Resource not found");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("error", "Resource not found");
    }

    @Test
    void handleRuntimeException_shouldReturnForbiddenForNotAuthorizedMessage() {
        RuntimeException ex = new RuntimeException("User not authorized to perform this action");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("status", 403);
        assertThat(response.getBody()).containsEntry("error", "Access denied");
    }

    @Test
    void handleRuntimeException_shouldReturnBadRequestForInvalidFileMessage() {
        RuntimeException ex = new RuntimeException("Invalid file type");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("error", "Invalid file");
    }

    @Test
    void handleRuntimeException_shouldReturnBadRequestForGenericMessage() {
        RuntimeException ex = new RuntimeException("Something went wrong");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("error", "Request failed");
    }

    @Test
    void handleRuntimeException_shouldHandleNullMessage() {
        RuntimeException ex = new RuntimeException((String) null);

        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Request failed");
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        Exception ex = new Exception("Unexpected error");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsEntry("error", "Internal server error");
        assertThat(response.getBody()).containsEntry("message", "An unexpected error occurred. Please try again later.");
    }

    @Test
    void handleGenericException_shouldIncludeTimestamp() {
        Exception ex = new Exception("Error");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody().get("timestamp")).isNotNull();
    }
}
package com.ecommerce.media.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for Media Service
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        Map<String, Object> response = buildErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File too large",
                "The uploaded file exceeds the maximum allowed size (2MB)"
        );
        log.warn("File upload size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();
        HttpStatus status;
        String errorType;

        if (message != null && message.toLowerCase().contains("not found")) {
            status = HttpStatus.NOT_FOUND;
            errorType = "Resource not found";
        } else if (message != null && message.toLowerCase().contains("not authorized")) {
            status = HttpStatus.FORBIDDEN;
            errorType = "Access denied";
        } else if (message != null && message.toLowerCase().contains("invalid file")) {
            status = HttpStatus.BAD_REQUEST;
            errorType = "Invalid file";
        } else {
            status = HttpStatus.BAD_REQUEST;
            errorType = "Request failed";
        }

        Map<String, Object> response = buildErrorResponse(status, errorType, message);
        log.error("Runtime exception: {}", message);
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        Map<String, Object> response = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred. Please try again later."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private Map<String, Object> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        return response;
    }
}
package com.ecommerce.user.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 *
 * Centralise la gestion des exceptions pour retourner des réponses cohérentes
 * et éviter la fuite d'informations sensibles.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors (from @Valid annotations)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation failed");
        response.put("details", fieldErrors);

        log.warn("Validation error: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                ex.getMessage()
        );
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle RuntimeException (custom business exceptions)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();

        // Determine appropriate status based on exception message
        HttpStatus status;
        String errorType;

        if (message != null && message.toLowerCase().contains("not found")) {
            status = HttpStatus.NOT_FOUND;
            errorType = "Resource not found";
        } else if (message != null && message.toLowerCase().contains("not authorized")) {
            status = HttpStatus.FORBIDDEN;
            errorType = "Access denied";
        } else if (message != null && message.toLowerCase().contains("already")) {
            status = HttpStatus.CONFLICT;
            errorType = "Conflict";
        } else {
            status = HttpStatus.BAD_REQUEST;
            errorType = "Request failed";
        }

        Map<String, Object> response = buildErrorResponse(status, errorType, message);
        log.error("Runtime exception: {}", message);
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Handle all other exceptions (catch-all)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Log the full stack trace for debugging but don't expose it
        log.error("Unexpected error occurred", ex);

        Map<String, Object> response = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred. Please try again later."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Build a standardized error response
     */
    private Map<String, Object> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        return response;
    }
}
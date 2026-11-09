package com.SwiftBasket.order_service.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private LocalDateTime timestamp;
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("NOT_FOUND")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OrderAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleOrderAccessDenied(OrderAccessDeniedException ex) {
        log.warn("Order access denied: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("FORBIDDEN")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(CartEmptyException.class)
    public ResponseEntity<ErrorResponse> handleCartEmpty(CartEmptyException ex) {
        log.warn("Checkout blocked: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        log.warn("Stock reservation failed: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("CONFLICT")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        log.warn("Invalid status transition: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized request: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("UNAUTHORIZED")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AdminAccessException.class)
    public ResponseEntity<ErrorResponse> handleAdminAccess(AdminAccessException ex) {
        log.warn("Admin access denied: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("FORBIDDEN")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({CartServiceUnavailableException.class, ProductUnavailableException.class})
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(RuntimeException ex) {
        log.error("Downstream service unavailable: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("SERVICE_UNAVAILABLE")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message("Validation failed: " + fieldErrors)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

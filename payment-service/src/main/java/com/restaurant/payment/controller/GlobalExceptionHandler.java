package com.restaurant.payment.controller;

import com.stripe.exception.StripeException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Data
    @Builder
    public static class ErrorResponse {
        private String message;
        private String code;
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ErrorResponse> handleStripeException(StripeException ex) {
        log.error("Stripe API exception caught: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .message(ex.getMessage())
                .code(ex.getCode())
                .build();
        return ResponseEntity.status(HttpStatus.valueOf(ex.getStatusCode() != null ? ex.getStatusCode() : 500))
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Validation error: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .message(ex.getMessage())
                .code("bad_request")
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unhandled exception caught: ", ex);
        ErrorResponse response = ErrorResponse.builder()
                .message("An unexpected error occurred. Please try again.")
                .code("internal_server_error")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

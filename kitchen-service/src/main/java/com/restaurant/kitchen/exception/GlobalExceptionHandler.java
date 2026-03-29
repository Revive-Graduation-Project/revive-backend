package com.restaurant.kitchen.exception;

import com.restaurant.kitchen.dto.ErrorResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Objects;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 404 — accessed endpoint does not exist
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundEndpoint(NoResourceFoundException e) {

        ErrorResponse error = new ErrorResponse(
                "Endpoint /" + e.getResourcePath() + " not found",
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    // 405 unsupported HTTP method
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {

        ErrorResponse error = new ErrorResponse(
                "Method '" + e.getMethod()
                        + "' is not supported for this endpoint. Supported methods: "
                        + e.getSupportedHttpMethods(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED) // 405
                .body(error);
    }

    // 404 — chef not found
    @ExceptionHandler(ChefNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChefNotFound(ChefNotFoundException e) {

        ErrorResponse error = new ErrorResponse(e.getMessage(), Instant.now());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    // 404 — ticket not found
    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTicketNotFound(TicketNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), Instant.now()));
    }

    // 409 — duplicate unique field
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {

        ErrorResponse error = new ErrorResponse("Resource already exists", Instant.now());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    //500 — database error
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException e) {
        log.error("Database error", e);

        ErrorResponse error = new ErrorResponse("A database error occurred", Instant.now());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }

    // 400 — validation failed (@NotNull, @NotBlank etc)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> Objects.requireNonNullElse(error.getDefaultMessage(), "Validation error"))  // gets message from annotation
                .findFirst()
                .orElse("Validation error"); // potential NoSuchElementException

        ErrorResponse error = new ErrorResponse(message, Instant.now());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    //400 — invalid JSON or enum value
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFormat(HttpMessageNotReadableException e) {

        ErrorResponse error = new ErrorResponse("Invalid value provided", Instant.now());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    // fallback — anything unexpected
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {

        log.error("Unexpected error", e);

        ErrorResponse error = new ErrorResponse(
                "An unexpected internal server error occurred. Please try again later."
                , Instant.now());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
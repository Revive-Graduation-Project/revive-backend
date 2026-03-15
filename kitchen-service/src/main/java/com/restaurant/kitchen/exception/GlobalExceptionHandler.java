package com.restaurant.kitchen.exception;

import com.restaurant.kitchen.dto.ErrorResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 404 — chef not found
    @ExceptionHandler(ChefNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChefNotFound(ChefNotFoundException e) {

        ErrorResponse error = new ErrorResponse(e.getMessage(), LocalDateTime.now().toString());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    // 404 — ticket not found
    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTicketNotFound(TicketNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), LocalDateTime.now().toString()));
    }

    // 403 - X-User-Role is forbidden
    @ExceptionHandler(ForbiddenRoleException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenial(ForbiddenRoleException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage(), LocalDateTime.now().toString()));
    }

    // 409 — duplicate unique field
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {

        ErrorResponse error = new ErrorResponse("Resource already exists", LocalDateTime.now().toString());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    //500 — database error
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException e) {
        log.error("Database error", e);

        ErrorResponse error = new ErrorResponse("A database error occurred", LocalDateTime.now().toString());

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
                .map(FieldError::getDefaultMessage)  // gets message from annotation
                .findFirst()
                .orElse("Validation error");

        ErrorResponse error = new ErrorResponse(message, LocalDateTime.now().toString());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    //400 — invalid JSON or enum value
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFormat(HttpMessageNotReadableException e) {

        ErrorResponse error = new ErrorResponse("Invalid value provided", LocalDateTime.now().toString());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }


    // fallback — anything unexpected
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {

        log.error("Unexpected error", e);

        ErrorResponse error = new ErrorResponse("Internal server error", LocalDateTime.now().toString());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }


}
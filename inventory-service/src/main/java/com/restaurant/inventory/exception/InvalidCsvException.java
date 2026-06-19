package com.restaurant.inventory.exception;

/**
 * Thrown when the uploaded CSV file is invalid.
 */
public class InvalidCsvException extends RuntimeException {
    
    public InvalidCsvException(String message) {
        super(message);
    }

    public InvalidCsvException(String message, Throwable cause) {
        super(message, cause);
    }
}

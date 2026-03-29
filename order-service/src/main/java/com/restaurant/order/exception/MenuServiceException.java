package com.restaurant.order.exception;

public class MenuServiceException extends RuntimeException {
    public MenuServiceException(String message) {
        super(message);
    }
}

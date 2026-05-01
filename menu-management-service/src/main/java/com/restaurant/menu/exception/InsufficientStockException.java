package com.restaurant.menu.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String ingredientName, double available, double requested) {
        super(String.format(
                "Insufficient stock for ingredient '%s': requested %.1fg but only %.1fg available",
                ingredientName, requested, available));
    }
}

package com.restaurant.kitchen.exception;

public class ChefNotFoundException extends RuntimeException {
    public ChefNotFoundException(Long id) {
        super("Chef not found with id: " + id);
    }
}
package com.restaurant.menu.exception;

public class MealNotFoundException extends RuntimeException {
    public MealNotFoundException(Long id) {
        super("Meal not found with id: " + id);
    }
}

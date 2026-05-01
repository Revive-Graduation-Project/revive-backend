package com.restaurant.menu.exception;

public class IngredientNotFoundException extends RuntimeException {
    public IngredientNotFoundException(Long id) {
        super("Ingredient not found with id: " + id);
    }
}

package com.restaurant.menu.dto;

import java.util.List;

/**
 * Mirrors the MealNutrition record from inventory-service
 * for deserialization of RabbitMQ messages.
 */
public record MealNutrition(String mealName, String category, double price, List<IngredientNutrition> ingredients) {
}

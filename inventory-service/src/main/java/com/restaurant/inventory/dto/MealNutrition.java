package com.restaurant.inventory.dto;

import java.util.List;

public record MealNutrition(String mealName, String category, double price, String description, List<IngredientNutrition> ingredients) {
}
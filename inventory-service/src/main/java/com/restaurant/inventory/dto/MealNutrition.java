package com.restaurant.inventory.dto;

import java.util.List;

public record MealNutrition(String mealName, String category, double price, List<IngredientNutrition> ingredients) {
}
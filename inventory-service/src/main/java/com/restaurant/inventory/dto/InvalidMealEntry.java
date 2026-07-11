package com.restaurant.inventory.dto;

public record InvalidMealEntry(int rowIndex, String mealName, String reason) {
}

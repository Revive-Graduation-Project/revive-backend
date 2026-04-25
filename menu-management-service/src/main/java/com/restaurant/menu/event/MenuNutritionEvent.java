package com.restaurant.menu.event;

import com.restaurant.menu.dto.MealNutrition;

import java.util.List;

/**
 * Event received from RabbitMQ, published by the inventory-service
 * after processing CSV nutrition data.
 */
public record MenuNutritionEvent(List<MealNutrition> meals) {
}

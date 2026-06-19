package com.restaurant.menu.dto;

/**
 * Mirrors the NutrientInfo record from inventory-service
 * for deserialization of RabbitMQ messages.
 */
public record NutrientInfo(String nutrientName, Double value, String unitName) {
}

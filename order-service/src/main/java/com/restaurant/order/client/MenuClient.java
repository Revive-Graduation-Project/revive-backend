package com.restaurant.order.client;

import com.restaurant.order.dto.request.OrderItemRequest;
import com.restaurant.order.dto.snapshot.MealPriceSnapshot;

import java.util.List;

public interface MenuClient {
    MealPriceSnapshot getMealById(Long mealId);
    List<com.restaurant.order.dto.IngredientDTO> getAllIngredients();
    void reserveStock(List<OrderItemRequest> items);
    void rollbackStock(List<OrderItemRequest> items);
}

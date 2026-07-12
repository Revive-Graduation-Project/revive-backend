package com.restaurant.order.client;

import com.restaurant.order.dto.request.OrderItemRequest;
import com.restaurant.order.dto.snapshot.MealPriceSnapshot;

import java.util.List;

public interface MenuClient {
    MealPriceSnapshot getMealById(Long mealId);
    List<com.restaurant.order.dto.IngredientDTO> getAllIngredients();
    void reserveStock(List<com.restaurant.order.dto.request.ReserveMealRequest> items);

    void rollbackStock(List<com.restaurant.order.dto.request.ReserveMealRequest> items);
}

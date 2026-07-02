package com.restaurant.order.client;

import com.restaurant.order.dto.request.OrderItemRequest;
import com.restaurant.order.dto.snapshot.MealPriceSnapshot;

import java.util.List;

public interface MenuClient {
    MealPriceSnapshot getMealById(Long mealId);
    void reserveStock(List<OrderItemRequest> items);
    void rollbackStock(List<OrderItemRequest> items);
}

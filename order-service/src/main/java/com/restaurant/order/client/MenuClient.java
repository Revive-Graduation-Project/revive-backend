package com.restaurant.order.client;

import com.restaurant.order.dto.snapshot.MealSnapshot;
import java.util.UUID;

public interface MenuClient {
    MealSnapshot getMealById(UUID mealId);
}

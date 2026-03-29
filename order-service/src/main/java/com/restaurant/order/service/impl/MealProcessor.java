package com.restaurant.order.service.impl;

import com.restaurant.order.client.MenuClient;
import com.restaurant.order.dto.request.OrderItemRequest;
import com.restaurant.order.dto.snapshot.MealSnapshot;
import com.restaurant.order.entity.Order;
import com.restaurant.order.entity.OrderItem;
import com.restaurant.order.service.ProductProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MealProcessor implements ProductProcessor<OrderItemRequest> {

    private final MenuClient menuClient;

    @Override
    public void process(Order order, OrderItemRequest request) {
        MealSnapshot meal = menuClient.getMealById(request.mealId());

        if (request.quantity() > meal.maxQuantity()) {
            throw new RuntimeException("Quantity " + request.quantity() + 
                    " exceeds maximum allowed (" + meal.maxQuantity() + ") for meal: " + meal.name());
        }

        OrderItem item = OrderItem.builder()
                .order(order)
                .mealId(meal.id())
                .snapshotName(meal.name())
                .snapshotPrice(meal.price())
                .quantity(request.quantity())
                .snapshotCalories(meal.totalCalories())
                .snapshotProtein(meal.totalProtein())
                .snapshotCarbs(meal.totalCarbs())
                .snapshotFats(meal.totalFats())
                .build();

        order.getItems().add(item);
    }

    @Override
    public boolean supports(Object request) {
        return request instanceof OrderItemRequest;
    }
}

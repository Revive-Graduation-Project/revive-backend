package com.restaurant.order.service.impl;

import com.restaurant.order.client.InventoryClient;
import com.restaurant.order.dto.request.CustomOrderItemRequest;
import com.restaurant.order.dto.snapshot.CustomIngredientSnapshot;
import com.restaurant.order.entity.CustomOrderItem;
import com.restaurant.order.entity.Order;
import com.restaurant.order.service.ProductProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomIngredientProcessor implements ProductProcessor<CustomOrderItemRequest> {

    private final InventoryClient inventoryClient;

    @Override
    public void process(Order order, CustomOrderItemRequest request) {
        CustomIngredientSnapshot ingredient = inventoryClient.getIngredientById(request.ingredientId());

        CustomOrderItem customItem = CustomOrderItem.builder()
                .order(order)
                .ingredientId(ingredient.id())
                .snapshotName(ingredient.name())
                .quantityGrams(ingredient.portionGrams())
                .snapshotPricePer100Gram(ingredient.pricePer100Gram())
                .snapshotCaloriesPer100Gram(ingredient.caloriesPer100Gram())
                .snapshotProteinPer100Gram(ingredient.proteinPer100Gram())
                .snapshotCarbsPer100Gram(ingredient.carbsPer100Gram())
                .snapshotFatsPer100Gram(ingredient.fatsPer100Gram())
                .build();

        order.getCustomItems().add(customItem);
    }

    @Override
    public boolean supports(Object request) {
        return request instanceof CustomOrderItemRequest;
    }
}

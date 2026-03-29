package com.restaurant.order.service;

import com.restaurant.order.entity.CustomOrderItem;
import com.restaurant.order.entity.Order;
import com.restaurant.order.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Component responsible only for calculating order totals (SRP).
 */
@Component
public class OrderCalculator {

    public void calculateTotals(Order order) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        double totalCalories = 0, totalProtein = 0, totalCarbs = 0, totalFats = 0;

        // Standard meal items
        for (OrderItem item : order.getItems()) {
            totalPrice = totalPrice.add(
                    item.getSnapshotPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            totalCalories += item.getSnapshotCalories() * item.getQuantity();
            totalProtein += item.getSnapshotProtein() * item.getQuantity();
            totalCarbs += item.getSnapshotCarbs() * item.getQuantity();
            totalFats += item.getSnapshotFats() * item.getQuantity();
        }

        // Custom ingredient items
        for (CustomOrderItem item : order.getCustomItems()) {
            BigDecimal pricePerGram = item.getSnapshotPricePer100Gram()
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            
            totalPrice = totalPrice.add(
                    pricePerGram.multiply(BigDecimal.valueOf(item.getQuantityGrams())));
            
            totalCalories += (item.getSnapshotCaloriesPer100Gram() / 100.0) * item.getQuantityGrams();
            totalProtein += (item.getSnapshotProteinPer100Gram() / 100.0) * item.getQuantityGrams();
            totalCarbs += (item.getSnapshotCarbsPer100Gram() / 100.0) * item.getQuantityGrams();
            totalFats += (item.getSnapshotFatsPer100Gram() / 100.0) * item.getQuantityGrams();
        }

        order.setTotalPrice(totalPrice.setScale(2, RoundingMode.HALF_UP));
        order.setTotalCalories(totalCalories);
        order.setTotalProtein(totalProtein);
        order.setTotalCarbs(totalCarbs);
        order.setTotalFats(totalFats);
    }
}

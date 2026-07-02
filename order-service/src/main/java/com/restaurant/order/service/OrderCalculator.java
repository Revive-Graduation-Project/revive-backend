package com.restaurant.order.service;

import com.restaurant.order.entity.Order;
import com.restaurant.order.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates order total price only.
 */
@Component
public class OrderCalculator {

    public void calculateTotals(Order order) {
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItem item : order.getItems()) {
            totalPrice = totalPrice.add(
                    item.getSnapshotPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // Apply point redemption discount
        if (order.getDiscount() != 0) {

            BigDecimal discountPercentage =
                    BigDecimal.valueOf(order.getDiscount())
                            .divide(BigDecimal.valueOf(100),
                                    2,
                                    RoundingMode.HALF_UP);

            BigDecimal discountAmount =
                    totalPrice.multiply(discountPercentage);

            totalPrice = totalPrice.subtract(discountAmount);
        }

        order.setTotalPrice(totalPrice.setScale(2, RoundingMode.HALF_UP));
    }
}

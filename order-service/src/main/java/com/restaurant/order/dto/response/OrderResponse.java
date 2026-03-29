package com.restaurant.order.dto.response;

import com.restaurant.order.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        OrderStatus status,
        BigDecimal totalPrice,
        Double totalCalories,
        Double totalProtein,
        Double totalCarbs,
        Double totalFats,
        LocalDateTime createdAt,
        List<OrderItemResponse> items,
        List<CustomOrderItemResponse> customItems
) {}

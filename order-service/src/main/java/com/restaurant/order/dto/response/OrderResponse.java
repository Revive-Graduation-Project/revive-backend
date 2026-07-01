package com.restaurant.order.dto.response;

import com.restaurant.order.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long clientId,
        OrderStatus status,
        BigDecimal totalPrice,
        Integer discount,
        LocalDateTime createdAt,
        String stripeClientSecret,
        String stripePaymentIntentId,
        List<OrderItemResponse> items
) {}

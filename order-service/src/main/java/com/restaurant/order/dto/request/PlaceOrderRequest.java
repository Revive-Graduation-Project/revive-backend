package com.restaurant.order.dto.request;

import com.restaurant.order.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PlaceOrderRequest(
        @Valid
        @NotEmpty(message = "Order must contain at least one item")
        List<OrderItemRequest> items,

        @Min(value = 0, message = "Points to redeem cannot be negative")
        Integer points,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod
) {}

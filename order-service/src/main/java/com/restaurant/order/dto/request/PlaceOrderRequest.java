package com.restaurant.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PlaceOrderRequest(
        @Valid
        @NotEmpty(message = "Order must contain at least one item")
        List<OrderItemRequest> items,

        @Valid
        List<CustomOrderItemRequest> customItems
) {}

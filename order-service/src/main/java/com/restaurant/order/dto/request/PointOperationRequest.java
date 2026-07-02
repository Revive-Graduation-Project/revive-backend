package com.restaurant.order.dto.request;

public record PointOperationRequest(
    Long customerId,
    Integer points
) {}

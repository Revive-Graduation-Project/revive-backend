package com.restaurant.order.service;

import com.restaurant.order.entity.Order;

/**
 * Strategy interface for processing different types of order items (OCP).
 * @param <T> The request type (OrderItemRequest or CustomOrderItemRequest)
 */
public interface ProductProcessor<T> {
    void process(Order order, T request);
    boolean supports(Object request);
}

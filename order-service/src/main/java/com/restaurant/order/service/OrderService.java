package com.restaurant.order.service;

import com.restaurant.order.dto.request.PlaceOrderRequest;
import com.restaurant.order.dto.response.OrderResponse;

public interface OrderService {

    OrderResponse placeOrder(PlaceOrderRequest request, Long customerId);

    void confirmOrder(Long orderId, Long ticketId);

    void cancelOrder(Long orderId, String reason);
    
    void onTicketReady(Long orderId);
}

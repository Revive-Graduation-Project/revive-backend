package com.restaurant.order.service;

import com.restaurant.order.dto.request.PlaceOrderRequest;
import com.restaurant.order.dto.response.OrderResponse;
import java.util.List;

public interface  OrderService {

    OrderResponse placeOrder(PlaceOrderRequest request, Long customerId);

    void confirmOrder(Long orderId, Long ticketId);

    void cancelOrder(Long orderId, String reason);
    
    void onTicketReady(Long orderId);

    void onTicketStarted(Long orderId, Long ticketId);

    void markPaymentSucceeded(Long orderId);

    void markPaymentFailed(Long orderId, String reason);

    void markPointRedemptionSucceeded(Long orderId);

    void markPointRedemptionFailed(Long orderId, String reason);

    OrderResponse retrieveOrder(Long orderId);

    List<OrderResponse> getClientOrderHistory(Long clientId);
}

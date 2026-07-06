package com.restaurant.order.service;

import com.restaurant.order.dto.response.OrderResponse;
import com.restaurant.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface AdminOrderService {
    Page<OrderResponse> getAllOrders(Pageable pageable, String status);
    Map<String, Object> getDailyMetrics();
    void updateOrderStatus(Long orderId, OrderStatus newStatus);
}

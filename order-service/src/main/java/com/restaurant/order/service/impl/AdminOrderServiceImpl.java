package com.restaurant.order.service.impl;

import com.restaurant.order.dto.response.OrderResponse;
import com.restaurant.order.entity.Order;
import com.restaurant.order.enums.OrderStatus;
import com.restaurant.order.mapper.OrderMapper;
import com.restaurant.order.repository.OrderRepository;
import com.restaurant.order.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable, String status) {
        Page<Order> orders;
        if (status != null && !status.isEmpty()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            orders = orderRepository.findByStatus(orderStatus, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }
        return orders.map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDailyMetrics() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        long totalOrders = orderRepository.countOrdersInDateRange(startOfDay, endOfDay);
        long preparing = orderRepository.countOrdersByStatusInDateRange(OrderStatus.PREPARING, startOfDay, endOfDay);
        long completed = orderRepository.countOrdersByStatusInDateRange(OrderStatus.DONE, startOfDay, endOfDay);
        BigDecimal salesCurrent = orderRepository.sumSalesByStatusInDateRange(OrderStatus.DONE, startOfDay, endOfDay);

        // Calculate goals (could be fetched from config or DB)
        long totalOrdersChange = 5; // Mock change percentage
        long preparingChange = -2; // Mock change percentage
        long completedChange = 12; // Mock change percentage
        BigDecimal salesTarget = BigDecimal.valueOf(10000); // Mock target
        long ordersTarget = 200; // Mock target

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalOrders", totalOrders);
        metrics.put("totalOrdersChange", totalOrdersChange);
        metrics.put("preparing", preparing);
        metrics.put("preparingChange", preparingChange);
        metrics.put("completed", completed);
        metrics.put("completedChange", completedChange);
        metrics.put("salesCurrent", salesCurrent);
        metrics.put("salesTarget", salesTarget);
        metrics.put("ordersCurrent", completed);
        metrics.put("ordersTarget", ordersTarget);

        Map<String, Object> dailyGoal = new HashMap<>();
        dailyGoal.put("salesCurrent", salesCurrent);
        dailyGoal.put("salesTarget", salesTarget);
        dailyGoal.put("ordersCurrent", completed);
        dailyGoal.put("ordersTarget", ordersTarget);

        metrics.put("dailyGoal", dailyGoal);

        return metrics;
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        order.setStatus(newStatus);
        orderRepository.save(order);
    }
}

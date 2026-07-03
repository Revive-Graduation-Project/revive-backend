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
            OrderStatus orderStatus;
            try {
                orderStatus = OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status: " + status);
            }
            orders = orderRepository.findByStatus(orderStatus, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }
        return orders.map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDailyMetrics() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        LocalDate yesterday = today.minusDays(1);
        LocalDateTime startOfYesterday = yesterday.atStartOfDay();
        LocalDateTime endOfYesterday = yesterday.atTime(LocalTime.MAX);

        long totalOrdersToday = orderRepository.countOrdersInDateRange(startOfDay, endOfDay);
        long totalOrdersYesterday = orderRepository.countOrdersInDateRange(startOfYesterday, endOfYesterday);

        long preparingToday = orderRepository.countOrdersByStatusInDateRange(OrderStatus.PREPARING, startOfDay, endOfDay);
        long preparingYesterday = orderRepository.countOrdersByStatusInDateRange(OrderStatus.PREPARING, startOfYesterday, endOfYesterday);

        long completedToday = orderRepository.countOrdersByStatusInDateRange(OrderStatus.DONE, startOfDay, endOfDay);
        long completedYesterday = orderRepository.countOrdersByStatusInDateRange(OrderStatus.DONE, startOfYesterday, endOfYesterday);

        BigDecimal salesToday = orderRepository.sumSalesByStatusInDateRange(OrderStatus.DONE, startOfDay, endOfDay);
        if (salesToday == null) salesToday = BigDecimal.ZERO;
        // BigDecimal salesYesterday = orderRepository.sumSalesByStatusInDateRange(OrderStatus.DONE, startOfYesterday, endOfYesterday);
        // if (salesYesterday == null) salesYesterday = BigDecimal.ZERO;

        // Calculate goals (these should ideally be injected via @Value, keeping hardcoded for this refactor to avoid changing too many files)
        BigDecimal salesTarget = BigDecimal.valueOf(10000); 
        long ordersTarget = 200; 

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalOrders", totalOrdersToday);
        metrics.put("totalOrdersChange", calculateChange(totalOrdersToday, totalOrdersYesterday));
        metrics.put("preparing", preparingToday);
        metrics.put("preparingChange", calculateChange(preparingToday, preparingYesterday));
        metrics.put("completed", completedToday);
        metrics.put("completedChange", calculateChange(completedToday, completedYesterday));
        metrics.put("salesCurrent", salesToday);
        metrics.put("salesTarget", salesTarget);
        metrics.put("ordersCurrent", completedToday);
        metrics.put("ordersTarget", ordersTarget);

        Map<String, Object> dailyGoal = new HashMap<>();
        dailyGoal.put("salesCurrent", salesToday);
        dailyGoal.put("salesTarget", salesTarget);
        dailyGoal.put("ordersCurrent", completedToday);
        dailyGoal.put("ordersTarget", ordersTarget);

        metrics.put("dailyGoal", dailyGoal);

        return metrics;
    }

    private long calculateChange(long today, long yesterday) {
        if (yesterday == 0) {
            return today == 0 ? 0 : 100;
        }
        return (long) (((double) (today - yesterday) / yesterday) * 100);
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

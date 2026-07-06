package com.restaurant.order.service;

import com.restaurant.order.dto.response.OrderResponse;
import com.restaurant.order.entity.Order;
import com.restaurant.order.enums.OrderStatus;
import com.restaurant.order.mapper.OrderMapper;
import com.restaurant.order.repository.OrderRepository;
import com.restaurant.order.service.impl.AdminOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    private AdminOrderServiceImpl adminOrderService;

    @BeforeEach
    void setUp() {
        adminOrderService = new AdminOrderServiceImpl(orderRepository, orderMapper);
    }

    @Test
    void getAllOrders_ReturnsAllOrders_WhenStatusIsNull() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Order order = Order.builder().id(1L).status(OrderStatus.PENDING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(orderRepository.findAll(pageRequest)).thenReturn(orderPage);
        when(orderMapper.toResponse(order)).thenReturn(new OrderResponse(1L, 100L, OrderStatus.PENDING, BigDecimal.TEN, 0, LocalDateTime.now(), null, null, List.of()));

        Page<OrderResponse> result = adminOrderService.getAllOrders(pageRequest, null);

        assertEquals(1, result.getTotalElements());
        verify(orderRepository).findAll(pageRequest);
        verify(orderRepository, never()).findByStatus(any(), any());
    }

    @Test
    void getAllOrders_ReturnsFilteredOrders_WhenStatusIsProvided() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Order order = Order.builder().id(1L).status(OrderStatus.PREPARING).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(orderRepository.findByStatus(OrderStatus.PREPARING, pageRequest)).thenReturn(orderPage);
        when(orderMapper.toResponse(order)).thenReturn(new OrderResponse(1L, 100L, OrderStatus.PREPARING, BigDecimal.TEN, 0, LocalDateTime.now(), null, null, List.of()));

        Page<OrderResponse> result = adminOrderService.getAllOrders(pageRequest, "PREPARING");

        assertEquals(1, result.getTotalElements());
        verify(orderRepository).findByStatus(OrderStatus.PREPARING, pageRequest);
    }

    @Test
    void getAllOrders_ThrowsBadRequest_WhenStatusIsInvalid() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> 
            adminOrderService.getAllOrders(pageRequest, "INVALID_STATUS")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(orderRepository, never()).findByStatus(any(), any());
    }

    @Test
    void getDailyMetrics_ReturnsCalculatedMetrics() {
        when(orderRepository.countOrdersInDateRange(any(), any())).thenReturn(150L).thenReturn(100L); // Today, Yesterday
        when(orderRepository.countOrdersByStatusInDateRange(eq(OrderStatus.PREPARING), any(), any())).thenReturn(10L).thenReturn(10L); // Today, Yesterday
        when(orderRepository.countOrdersByStatusInDateRange(eq(OrderStatus.DONE), any(), any())).thenReturn(100L).thenReturn(50L); // Today, Yesterday
        
        when(orderRepository.sumSalesByStatusInDateRange(eq(OrderStatus.DONE), any(), any()))
                .thenReturn(BigDecimal.valueOf(5000))
                .thenReturn(BigDecimal.valueOf(2500)); // Today, Yesterday

        Map<String, Object> metrics = adminOrderService.getDailyMetrics();

        assertEquals(150L, metrics.get("totalOrders"));
        assertEquals(50L, metrics.get("totalOrdersChange")); // (150 - 100) / 100 * 100

        assertEquals(10L, metrics.get("preparing"));
        assertEquals(0L, metrics.get("preparingChange")); // (10 - 10) / 10 * 100

        assertEquals(100L, metrics.get("completed"));
        assertEquals(100L, metrics.get("completedChange")); // (100 - 50) / 50 * 100

        assertEquals(BigDecimal.valueOf(5000), metrics.get("salesCurrent"));
        assertEquals(100L, metrics.get("salesChange")); // (5000 - 2500) / 2500 * 100
        assertEquals(BigDecimal.valueOf(10000), metrics.get("salesTarget")); // Configurable target
        
        assertEquals(100L, metrics.get("ordersCurrent"));
        assertEquals(200L, metrics.get("ordersTarget"));
    }

    @Test
    void updateOrderStatus_UpdatesStatus_WhenOrderExists() {
        Order order = Order.builder().id(1L).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        adminOrderService.updateOrderStatus(1L, OrderStatus.PREPARING);

        assertEquals(OrderStatus.PREPARING, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatus_ThrowsException_WhenOrderNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> 
            adminOrderService.updateOrderStatus(1L, OrderStatus.PREPARING)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(orderRepository, never()).save(any());
    }
}

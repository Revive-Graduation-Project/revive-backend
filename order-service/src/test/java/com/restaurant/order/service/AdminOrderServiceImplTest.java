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
        when(orderMapper.toResponse(order)).thenReturn(new OrderResponse(1L, 100L, "John Doe", OrderStatus.PENDING, BigDecimal.TEN, 0, null, null, null, List.of()));

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
        when(orderMapper.toResponse(order)).thenReturn(new OrderResponse(1L, 100L, "John Doe", OrderStatus.PREPARING, BigDecimal.TEN, 0, null, null, null, List.of()));

        Page<OrderResponse> result = adminOrderService.getAllOrders(pageRequest, "PREPARING");

        assertEquals(1, result.getTotalElements());
        verify(orderRepository).findByStatus(OrderStatus.PREPARING, pageRequest);
    }

    @Test
    void getDailyMetrics_ReturnsCalculatedMetrics() {
        when(orderRepository.countOrdersInDateRange(any(), any())).thenReturn(150L);
        when(orderRepository.countOrdersByStatusInDateRange(eq(OrderStatus.PREPARING), any(), any())).thenReturn(10L);
        when(orderRepository.countOrdersByStatusInDateRange(eq(OrderStatus.DONE), any(), any())).thenReturn(100L);
        when(orderRepository.sumSalesByStatusInDateRange(eq(OrderStatus.DONE), any(), any())).thenReturn(BigDecimal.valueOf(5000));

        Map<String, Object> metrics = adminOrderService.getDailyMetrics();

        assertEquals(150L, metrics.get("totalOrders"));
        assertEquals(10L, metrics.get("preparing"));
        assertEquals(100L, metrics.get("completed"));
        assertEquals(BigDecimal.valueOf(5000), metrics.get("salesCurrent"));
        assertEquals(BigDecimal.valueOf(10000), metrics.get("salesTarget")); // Assuming hardcoded target for now
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

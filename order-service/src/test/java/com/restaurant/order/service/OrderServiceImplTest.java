package com.restaurant.order.service;

import com.restaurant.order.dto.request.CustomOrderItemRequest;
import com.restaurant.order.dto.request.OrderItemRequest;
import com.restaurant.order.dto.request.PlaceOrderRequest;
import com.restaurant.order.dto.response.OrderResponse;
import com.restaurant.order.dto.snapshot.CustomIngredientSnapshot;
import com.restaurant.order.dto.snapshot.MealSnapshot;
import com.restaurant.order.entity.Order;
import com.restaurant.order.enums.OrderStatus;
import com.restaurant.order.events.OrderCreatedEvent;
import com.restaurant.order.mapper.OrderMapper;
import com.restaurant.order.messaging.MessagePublisher;
import com.restaurant.order.repository.OrderRepository;
import com.restaurant.order.service.impl.MealProcessor;
import com.restaurant.order.service.impl.CustomIngredientProcessor;
import com.restaurant.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private OrderMapper orderMapper;

    @Mock private OrderCalculator orderCalculator;
    @Mock private List<ProductProcessor<?>> processors;
    @Mock private com.restaurant.order.client.InventoryClient inventoryClient;
    @Mock private com.restaurant.order.client.MenuClient menuClient;
    @Mock private MealProcessor mealProcessor;
    @Mock private CustomIngredientProcessor customProcessor;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        // Manually initialize to handle the List of processors and the new calculator
        orderService = new OrderServiceImpl(
                orderRepository, 
                messagePublisher, 
                orderMapper, 
                orderCalculator, 
                List.of(mealProcessor, customProcessor),
                inventoryClient,
                menuClient
        );
    }

    // ── Helpers ──────────────────────────────────────────────────

    private OrderResponse buildDummyResponse() {
        return new OrderResponse(
                1L, 100L, OrderStatus.PENDING, BigDecimal.valueOf(15.99),
                450.0, 35.0, 40.0, 12.0, null,
                Collections.emptyList(), Collections.emptyList());
    }

    // ── Tests ────────────────────────────────────────────────────

    @Test
    void placeOrder_withStandardMeals_snapshotsAndSaves() {
        UUID mealId = UUID.randomUUID();
        OrderItemRequest itemReq = new OrderItemRequest(mealId, 2);
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(itemReq), null);

        // Mock processor support and behavior
        when(mealProcessor.supports(itemReq)).thenReturn(true);
        
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });
        when(orderMapper.toResponse(any(Order.class))).thenReturn(buildDummyResponse());

        orderService.placeOrder(request, 100L);

        verify(mealProcessor).process(any(Order.class), eq(itemReq));
        verify(orderCalculator).calculateTotals(any(Order.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void placeOrder_exceedingMaxQuantity_throwsException() {
        UUID mealId = UUID.randomUUID();
        OrderItemRequest itemReq = new OrderItemRequest(mealId, 15); // Exceeds default max 10
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(itemReq), null);

        when(mealProcessor.supports(itemReq)).thenReturn(true);
        doThrow(new RuntimeException("Quantity exceeds maximum")).when(mealProcessor).process(any(), any());

        assertThrows(RuntimeException.class, () -> orderService.placeOrder(request, 100L));
    }

    @Test
    void placeOrder_withCustomIngredients_snapshotsAndSaves() {
        UUID ingredientId = UUID.randomUUID();
        CustomOrderItemRequest customReq = new CustomOrderItemRequest(ingredientId);
        PlaceOrderRequest request = new PlaceOrderRequest(null, List.of(customReq));

        when(customProcessor.supports(customReq)).thenReturn(true);
        
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(2L);
            return o;
        });
        when(orderMapper.toResponse(any(Order.class))).thenReturn(buildDummyResponse());

        orderService.placeOrder(request, 100L);

        verify(customProcessor).process(any(Order.class), eq(customReq));
        verify(orderCalculator).calculateTotals(any(Order.class));
    }

    @Test
    void placeOrder_withMixedItems_coordinatesProcessorsAndCalculator() {
        OrderItemRequest mealReq = new OrderItemRequest(UUID.randomUUID(), 2);
        CustomOrderItemRequest customReq = new CustomOrderItemRequest(UUID.randomUUID());
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(mealReq), List.of(customReq));

        when(mealProcessor.supports(mealReq)).thenReturn(true);
        when(customProcessor.supports(customReq)).thenReturn(true);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(3L);
            return o;
        });
        when(orderMapper.toResponse(any(Order.class))).thenReturn(buildDummyResponse());

        orderService.placeOrder(request, 100L);

        verify(mealProcessor).process(any(Order.class), eq(mealReq));
        verify(customProcessor).process(any(Order.class), eq(customReq));
        verify(orderCalculator).calculateTotals(any(Order.class));
    }

    @Test
    void placeOrder_publishesOrderCreatedEvent() {
        PlaceOrderRequest request = new PlaceOrderRequest(null, null);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(5L);
            return o;
        });
        when(orderMapper.toResponse(any(Order.class))).thenReturn(buildDummyResponse());

        orderService.placeOrder(request, 100L);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(messagePublisher).publishOrderCreated(eventCaptor.capture(), anyString(), anyString());

        assertEquals(5L, eventCaptor.getValue().getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void confirmOrder_updatesStatus() {
        Order order = Order.builder().id(10L).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(10L)).thenReturn(java.util.Optional.of(order));

        orderService.confirmOrder(10L, 200L);

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orderRepository).findById(10L);
    }

    @Test
    void cancelOrder_updatesStatus() {
        Order order = Order.builder().id(10L).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(10L)).thenReturn(java.util.Optional.of(order));

        orderService.cancelOrder(10L, "Stock issues");

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository).findById(10L);
    }
}

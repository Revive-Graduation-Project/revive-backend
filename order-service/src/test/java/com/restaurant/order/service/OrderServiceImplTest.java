package com.restaurant.order.service;

import com.restaurant.order.client.MenuClient;
import com.restaurant.order.client.PaymentServiceClient;
import com.restaurant.order.dto.request.OrderItemRequest;
import com.restaurant.order.dto.request.PlaceOrderRequest;
import com.restaurant.order.dto.response.OrderResponse;
import com.restaurant.order.dto.snapshot.MealPriceSnapshot;
import com.restaurant.order.entity.Order;
import com.restaurant.order.enums.OrderStatus;
import com.restaurant.order.exception.PointsException;
import com.restaurant.order.mapper.OrderMapper;
import com.restaurant.order.messaging.MessagePublisher;
import com.restaurant.order.repository.OrderRepository;
import com.restaurant.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private MessagePublisher messagePublisher;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderCalculator orderCalculator;
    @Mock private MenuClient menuClient;
    @Mock private PaymentServiceClient paymentServiceClient;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                messagePublisher,
                orderMapper,
                orderCalculator,
                menuClient,
                paymentServiceClient
        );
    }

    // ── Helpers ──────────────────────────────────────────────────

    private OrderResponse buildDummyResponse() {
        return new OrderResponse(1L, 100L, OrderStatus.PENDING,
                BigDecimal.valueOf(15.99), 0, null, null, Collections.emptyList());
    }

    private MealPriceSnapshot buildSnapshot(Long mealId) {
        return new MealPriceSnapshot(mealId, "Test Meal", BigDecimal.valueOf(9.99));
    }

    // ── Tests ────────────────────────────────────────────────────

    @Test
    void placeOrder_withValidPoints_fetchesPriceAndSavesOrder() {
        Long mealId = 1L;
        OrderItemRequest itemReq = new OrderItemRequest(mealId, 2);
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(itemReq), 100);

        when(menuClient.getMealById(mealId)).thenReturn(buildSnapshot(mealId));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            o.setClientId(100L);
            return o;
        });
        when(orderMapper.toResponse(any(Order.class))).thenReturn(buildDummyResponse());

        OrderResponse response = orderService.placeOrder(request, 100L);

        assertNotNull(response);
        verify(menuClient).getMealById(mealId);
        verify(orderCalculator).calculateTotals(any(Order.class));
        verify(menuClient).reserveStock(List.of(itemReq));
        verify(orderRepository, atLeastOnce()).save(any(Order.class));
        verify(messagePublisher).publishPointRedemptionRequested(any(), anyString(), anyString());
    }

    @Test
    void placeOrder_withInvalidPoints_throwsPointsException() {
        PlaceOrderRequest request = new PlaceOrderRequest(Collections.emptyList(), 50);

        assertThrows(PointsException.class, () -> orderService.placeOrder(request, 100L));
        verifyNoInteractions(menuClient, orderRepository, messagePublisher);
    }

    @Test
    void placeOrder_withNoPoints_createsPaymentIntentSync() {
        Long mealId = 1L;
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(new OrderItemRequest(mealId, 1)), 0);

        when(menuClient.getMealById(mealId)).thenReturn(buildSnapshot(mealId));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(5L);
            o.setTotalPrice(BigDecimal.valueOf(9.99));
            return o;
        });
        when(orderMapper.toResponse(any(Order.class))).thenReturn(buildDummyResponse());
        when(paymentServiceClient.createPaymentIntent(anyLong(), anyLong(), any(), anyString()))
                .thenReturn(new PaymentServiceClient.PaymentIntentResponse("secret_123", "pi_123", "requires_payment_method"));

        orderService.placeOrder(request, 100L);

        verify(paymentServiceClient).createPaymentIntent(anyLong(), anyLong(), any(), anyString());
        verify(messagePublisher, never()).publishPointRedemptionRequested(any(), anyString(), anyString());
    }

    @Test
    void placeOrder_dbFailure_rollsBackStock() {
        Long mealId = 1L;
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(new OrderItemRequest(mealId, 1)), 0);

        when(menuClient.getMealById(mealId)).thenReturn(buildSnapshot(mealId));
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("DB Down"));

        assertThrows(RuntimeException.class, () -> orderService.placeOrder(request, 100L));

        verify(menuClient).reserveStock(any());
        verify(menuClient).rollbackStock(any());
    }

    @Test
    void confirmOrder_updatesStatusToConfirmed() {
        Order order = Order.builder().id(10L).totalPrice(BigDecimal.valueOf(25)).status(OrderStatus.PAID).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.confirmOrder(10L, 200L);

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void confirmOrder_alreadyConfirmed_skips() {
        Order order = Order.builder().id(10L).status(OrderStatus.CONFIRMED).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.confirmOrder(10L, 200L);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void markPaymentSucceeded_updatesStatusAndNotifiesKitchen() {
        Order order = Order.builder().id(10L).status(OrderStatus.AWAITING_PAYMENT).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.markPaymentSucceeded(10L);

        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderRepository).save(order);
        verify(messagePublisher).publishOrderCreated(any(), anyString(), anyString());
    }

    @Test
    void markPaymentSucceeded_alreadyPaid_skips() {
        Order order = Order.builder().id(10L).status(OrderStatus.PAID).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.markPaymentSucceeded(10L);

        verify(orderRepository, never()).save(any());
        verify(messagePublisher, never()).publishOrderCreated(any(), anyString(), anyString());
    }

    @Test
    void markPointRedemptionSucceeded_updatesStatusToAwaitingPayment() {
        Order order = Order.builder().id(10L).clientId(100L).totalPrice(BigDecimal.valueOf(25)).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentServiceClient.createPaymentIntent(anyLong(), anyLong(), any(), anyString()))
                .thenReturn(new PaymentServiceClient.PaymentIntentResponse("secret_123", "pi_123", "requires_payment_method"));

        orderService.markPointRedemptionSucceeded(10L);

        assertEquals(OrderStatus.AWAITING_PAYMENT, order.getStatus());
        verify(orderRepository, atLeastOnce()).save(order);
        verify(paymentServiceClient).createPaymentIntent(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void markPointRedemptionSucceeded_alreadyProcessed_skips() {
        Order order = Order.builder().id(10L).status(OrderStatus.AWAITING_PAYMENT).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.markPointRedemptionSucceeded(10L);

        verify(orderRepository, never()).save(any());
        verify(paymentServiceClient, never()).createPaymentIntent(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void onTicketReady_updatesStatusToReadyForPickup() {
        Order order = Order.builder().id(10L).status(OrderStatus.CONFIRMED).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.onTicketReady(10L);

        assertEquals(OrderStatus.READY_FOR_PICKUP, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void onTicketReady_alreadyReady_skips() {
        Order order = Order.builder().id(10L).status(OrderStatus.READY_FOR_PICKUP).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.onTicketReady(10L);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_rollsBackStockAndUpdatesStatus() {
        Order order = Order.builder().id(10L).status(OrderStatus.PAID).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(10L, "Manual cancel");

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(menuClient).rollbackStock(any());
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_alreadyCancelled_skips() {
        Order order = Order.builder().id(10L).status(OrderStatus.CANCELLED).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(10L, "Double cancel");

        verify(menuClient, never()).rollbackStock(any());
        verify(orderRepository, never()).save(any());
    }
}

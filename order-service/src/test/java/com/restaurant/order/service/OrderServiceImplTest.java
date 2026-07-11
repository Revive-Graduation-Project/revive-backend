package com.restaurant.order.service;

import com.restaurant.order.client.MenuClient;
import com.restaurant.order.client.PaymentServiceClient;
import com.restaurant.order.dto.request.OrderItemRequest;
import com.restaurant.order.dto.request.PlaceOrderRequest;
import com.restaurant.order.dto.response.OrderResponse;
import com.restaurant.order.entity.Order;
import com.restaurant.order.enums.OrderStatus;
import com.restaurant.order.enums.PaymentMethod;
import com.restaurant.order.mapper.OrderMapper;
import com.restaurant.order.messaging.MessagePublisher;
import com.restaurant.order.repository.OrderRepository;
import com.restaurant.order.saga.OrderPlacementSaga;
import com.restaurant.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    @Mock private MenuClient menuClient;
    @Mock private PaymentServiceClient paymentServiceClient;
    @Mock private OrderPlacementSaga orderPlacementSaga;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                messagePublisher,
                orderMapper,
                menuClient,
                paymentServiceClient,
                orderPlacementSaga
        );
    }

    // ── Tests ────────────────────────────────────────────────────

    @Test
    void placeOrder_delegatesToOrderPlacementSaga() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(1L, null, 1)),
                0,
                PaymentMethod.CASH
        );
        OrderResponse expected = new OrderResponse(1L, 42L, OrderStatus.PENDING,
                BigDecimal.TEN, 0, null, null, null, List.of());
        when(orderPlacementSaga.execute(request, 42L)).thenReturn(expected);

        OrderResponse result = orderService.placeOrder(request, 42L);

        assertEquals(expected, result);
        verify(orderPlacementSaga).execute(request, 42L);
    }

    @Test
    void markPointRedemptionSucceeded_updatesStatusToAwaitingPayment() {
        Order order = Order.builder()
                .id(10L)
                .clientId(100L)
                .paymentMethod(PaymentMethod.CREDIT_CARD) // Using CREDIT_CARD
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentServiceClient.createPaymentIntent(anyLong(), anyLong(), any(), anyString()))
                .thenReturn(new PaymentServiceClient.PaymentIntentResponse("secret", "pi", "requires_payment"));

        orderService.markPointRedemptionSucceeded(10L);

        assertEquals(OrderStatus.AWAITING_PAYMENT, order.getStatus());
        verify(paymentServiceClient).createPaymentIntent(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void processTicketCancellationSuccess_updatesStatusToCanceled() {
        Order order = Order.builder().id(10L).status(OrderStatus.CANCELLATION_PENDING).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.processTicketCancellationSuccess(10L);

        assertEquals(OrderStatus.CANCELED, order.getStatus());
        verify(orderRepository).save(order);
        // Verify rollback happened
        verify(menuClient).rollbackStock(any());
    }

    @Test
    void processTicketCancellationFailure_revertsStatusToConfirmed() {
        Order order = Order.builder().id(10L).status(OrderStatus.CANCELLATION_PENDING).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.processTicketCancellationFailure(10L, "Chef is busy");

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void transactionalHelpers_arePublicAndTransactional() throws NoSuchMethodException {
        var cancelOrderInDb = OrderServiceImpl.class.getMethod("cancelOrderInDb", Long.class);
        assertNotNull(cancelOrderInDb.getAnnotation(org.springframework.transaction.annotation.Transactional.class));

        var processTicketCancellationSuccessInDb = OrderServiceImpl.class.getMethod("processTicketCancellationSuccessInDb", Long.class);
        assertNotNull(processTicketCancellationSuccessInDb.getAnnotation(org.springframework.transaction.annotation.Transactional.class));
    }

    @Test
    void markPointRedemptionSucceeded_isNotTransactional() throws NoSuchMethodException {
        var method = OrderServiceImpl.class.getMethod("markPointRedemptionSucceeded", Long.class);
        assertNull(method.getAnnotation(org.springframework.transaction.annotation.Transactional.class));
    }

    @Test
    void cancelOrder_and_processTicketCancellationSuccess_areNotTransactional() throws NoSuchMethodException {
        var cancelOrder = OrderServiceImpl.class.getMethod("cancelOrder", Long.class, String.class);
        assertNull(cancelOrder.getAnnotation(org.springframework.transaction.annotation.Transactional.class));

        var processTicketCancellationSuccess = OrderServiceImpl.class.getMethod("processTicketCancellationSuccess", Long.class);
        assertNull(processTicketCancellationSuccess.getAnnotation(org.springframework.transaction.annotation.Transactional.class));
    }

    @Test
    void cancelOrder_withPaidStatus_executesCompensationsWithPaymentSucceededTrue() {
        Order order = Order.builder()
                .id(20L)
                .status(OrderStatus.PAID)
                .discount(0)
                .build();

        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(20L, "User requested");

        assertEquals(OrderStatus.CANCELED, order.getStatus());
        verify(orderRepository).save(order);
        verify(menuClient).rollbackStock(any());
        verify(messagePublisher).publishPaymentRefund(any(), anyString(), anyString());
    }

    @Test
    void cancelOrder_withAwaitingPaymentStatus_executesCompensationsWithPaymentSucceededFalse() {
        Order order = Order.builder()
                .id(20L)
                .status(OrderStatus.AWAITING_PAYMENT)
                .discount(0)
                .build();

        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(20L, "User requested");

        assertEquals(OrderStatus.CANCELED, order.getStatus());
        verify(orderRepository).save(order);
        verify(menuClient).rollbackStock(any());
        verify(messagePublisher, never()).publishPaymentRefund(any(), anyString(), anyString());
    }

    @Test
    void cancelOrder_withPreparingStatus_throwsConflictResponseStatusException() {
        Order order = Order.builder()
                .id(30L)
                .status(OrderStatus.PREPARING)
                .build();

        when(orderRepository.findById(30L)).thenReturn(Optional.of(order));

        var ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> orderService.cancelOrder(30L, "User requested")
        );
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
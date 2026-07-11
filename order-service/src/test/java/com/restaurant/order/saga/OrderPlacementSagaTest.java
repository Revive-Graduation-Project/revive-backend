package com.restaurant.order.saga;

import com.restaurant.order.client.MenuClient;
import com.restaurant.order.client.PaymentServiceClient;
import com.restaurant.order.dto.IngredientDTO;
import com.restaurant.order.dto.request.OrderItemRequest;
import com.restaurant.order.dto.request.PlaceOrderRequest;
import com.restaurant.order.dto.response.OrderResponse;
import com.restaurant.order.dto.snapshot.MealPriceSnapshot;
import com.restaurant.order.entity.Order;
import com.restaurant.order.enums.OrderStatus;
import com.restaurant.order.enums.PaymentMethod;
import com.restaurant.order.exception.MenuServiceException;
import com.restaurant.order.mapper.OrderMapper;
import com.restaurant.order.messaging.MessagePublisher;
import com.restaurant.order.repository.OrderRepository;
import com.restaurant.order.service.OrderCalculator;
import com.restaurant.order.service.OrderDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for OrderPlacementSaga.execute — verifies orchestration and compensation behavior.
 * Seam: OrderPlacementSaga.execute(request, clientId)
 */
@ExtendWith(MockitoExtension.class)
class OrderPlacementSagaTest {

    @Mock private MenuClient menuClient;
    @Mock private PaymentServiceClient paymentServiceClient;
    @Mock private OrderRepository orderRepository;
    @Mock private MessagePublisher messagePublisher;
    @Mock private OrderMapper orderMapper;

    private OrderPlacementSaga saga;

    private static final Long CLIENT_ID = 42L;
    private static final Long ORDER_ID  = 1L;

    @BeforeEach
    void setUp() {
        OrderDomainService domainService = new OrderDomainService(new OrderCalculator());
        saga = new OrderPlacementSaga(
                menuClient, paymentServiceClient,
                orderRepository, messagePublisher,
                orderMapper, domainService
        );
        org.springframework.test.util.ReflectionTestUtils.setField(saga, "self", saga);
    }

    // ── Happy path: CASH payment, no points ────────────────────────────────────

    @Test
    void execute_cashPayment_noPoints_publishesOrderCreatedEvent() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(1L, null, 1)),
                0,
                PaymentMethod.CASH
        );
        stubMealSnapshot(1L, BigDecimal.valueOf(20.00));
        stubOrderSave();
        stubMapper();

        saga.execute(request, CLIENT_ID);

        verify(menuClient).reserveStock(any());
        verify(messagePublisher).publishOrderCreated(any(), anyString(), anyString());
        verify(paymentServiceClient, never()).createPaymentIntent(any(), any(), any(), any());
    }

    // ── Happy path: CREDIT_CARD, no points → createPaymentIntent ──────────────

    @Test
    void execute_creditCardPayment_noPoints_createsPaymentIntent() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(1L, null, 1)),
                0,
                PaymentMethod.CREDIT_CARD
        );
        stubMealSnapshot(1L, BigDecimal.valueOf(30.00));
        stubOrderSave();
        stubMapper();
        when(paymentServiceClient.createPaymentIntent(any(), any(), any(), any()))
                .thenReturn(new PaymentServiceClient.PaymentIntentResponse("secret", "pi_123", "requires_payment_method"));

        saga.execute(request, CLIENT_ID);

        verify(paymentServiceClient).createPaymentIntent(eq(ORDER_ID), eq(CLIENT_ID), any(), eq("egp"));
        verify(messagePublisher, never()).publishOrderCreated(any(), anyString(), anyString());
    }

    // ── Happy path: CREDIT_CARD + 100 points → PointRedemption event ──────────

    @Test
    void execute_creditCardWithPoints_publishesPointRedemptionEvent() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(1L, null, 1)),
                100,
                PaymentMethod.CREDIT_CARD
        );
        stubMealSnapshot(1L, BigDecimal.valueOf(50.00));
        stubOrderSave();
        stubMapper();

        saga.execute(request, CLIENT_ID);

        verify(messagePublisher).publishPointRedemptionRequested(any(), anyString(), anyString());
        verify(paymentServiceClient, never()).createPaymentIntent(any(), any(), any(), any());
        verify(messagePublisher, never()).publishOrderCreated(any(), anyString(), anyString());
    }

    // ── Compensation: stock reservation fails → rollbackStock NOT called ───────

    @Test
    void execute_stockReservationFails_doesNotCallRollback() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(1L, null, 1)),
                0,
                PaymentMethod.CASH
        );
        stubMealSnapshot(1L, BigDecimal.valueOf(20.00));
        doThrow(new MenuServiceException("Out of stock")).when(menuClient).reserveStock(any());

        assertThrows(MenuServiceException.class, () -> saga.execute(request, CLIENT_ID));

        // Nothing was on the stack yet — rollback should NOT be triggered
        verify(menuClient, never()).rollbackStock(any());
        verify(orderRepository, never()).save(any());
    }

    // ── Compensation: payment intent fails → rollbackStock IS called ───────────

    @Test
    void execute_paymentIntentFails_rollsBackStockAndMarksOrderFailed() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(1L, null, 1)),
                0,
                PaymentMethod.CREDIT_CARD
        );
        stubMealSnapshot(1L, BigDecimal.valueOf(40.00));
        stubOrderSave();
        when(paymentServiceClient.createPaymentIntent(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Stripe unavailable"));
        // stub findById for markOrderAsFailed compensation
        Order savedOrder = Order.builder().id(ORDER_ID).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));

        assertThrows(RuntimeException.class, () -> saga.execute(request, CLIENT_ID));

        verify(menuClient).rollbackStock(any());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void stubMealSnapshot(Long mealId, BigDecimal price) {
        when(menuClient.getMealById(mealId))
                .thenReturn(new MealPriceSnapshot(mealId, "Test Meal", price, null));
    }

    private void stubOrderSave() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(ORDER_ID);
            return o;
        });
    }

    private void stubMapper() {
        when(orderMapper.toResponse(any(Order.class)))
                .thenReturn(new OrderResponse(ORDER_ID, CLIENT_ID, OrderStatus.PENDING,
                        BigDecimal.TEN, 0, null, null, null, List.of()));
    }
}

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
import com.restaurant.order.events.OrderCreatedEvent;
import com.restaurant.order.events.payments.PaymentRefundRequestedEvent;
import com.restaurant.order.events.points.PointRedemptionRequestedEvent;
import com.restaurant.order.events.points.PointRedemptionRollbackRequestedEvent;
import com.restaurant.order.mapper.OrderMapper;
import com.restaurant.order.messaging.MessagePublisher;
import com.restaurant.order.repository.OrderRepository;
import com.restaurant.order.service.OrderDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates the distributed transaction of placing an order.
 * <p>
 * Uses a Compensation Stack: as each step succeeds its rollback is pushed
 * to the stack. On any failure the stack is unwound — no nested try-catches.
 * Contains no business rules; those live in {@link OrderDomainService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderPlacementSaga {

    private final MenuClient menuClient;
    private final PaymentServiceClient paymentServiceClient;
    private final OrderRepository orderRepository;
    private final MessagePublisher messagePublisher;
    private final OrderMapper orderMapper;
    private final OrderDomainService orderDomainService;

    @Autowired
    @Lazy
    private OrderPlacementSaga self;

    /**
     * Synchronously places an order. The API contract is unchanged — always returns
     * an {@link OrderResponse} on success.
     *
     * @param request  the validated placement request
     * @param clientId the authenticated client's ID
     * @return the persisted order response
     */
    public OrderResponse execute(PlaceOrderRequest request, Long clientId) {
        log.info("OrderPlacementSaga: starting for clientId={}", clientId);

        // ── Step 1: Fetch price snapshots (outside any DB transaction) ─────────
        Map<Long, MealPriceSnapshot> snapshots = fetchSnapshots(request);
        Map<Long, IngredientDTO> ingredientMap = fetchIngredients(request);

        // ── Step 2: Build the order via pure domain service ───────────────────
        Order order = orderDomainService.buildOrder(request, clientId, snapshots, ingredientMap);

        // ── Compensation stack ─────────────────────────────────────────────────
        Deque<Runnable> compensations = new ArrayDeque<>();
        try {

            // Step 3: Reserve stock
            List<com.restaurant.order.dto.request.ReserveMealRequest> stockItems = toStockItems(order);
            menuClient.reserveStock(stockItems);
            compensations.push(() -> {
                try {
                    menuClient.rollbackStock(stockItems);
                } catch (Exception e) {
                    log.error("Compensation: rollbackStock failed", e);
                }
            });

            // Step 4: Persist initial order
            Order saved = self.saveInitialOrder(order);
            log.info("OrderPlacementSaga: order saved id={}", saved.getId());
            compensations.push(() -> self.markOrderAsFailed(saved.getId()));

            // Step 5: Branch on payment/points
            int pointsToRedeem = request.points() != null ? request.points() : 0;
            if (pointsToRedeem > 0) {
                log.info("OrderPlacementSaga: triggering point redemption for {} points", pointsToRedeem);
                messagePublisher.publishPointRedemptionRequested(
                        new PointRedemptionRequestedEvent(saved.getId(), clientId, pointsToRedeem),
                        UUID.randomUUID().toString(), UUID.randomUUID().toString());
                compensations.push(() -> {
                    try {
                        messagePublisher.publishPointRedemptionRollback(
                                new PointRedemptionRollbackRequestedEvent(saved.getId(), clientId, pointsToRedeem),
                                UUID.randomUUID().toString(), UUID.randomUUID().toString());
                    } catch (Exception e) {
                        log.error("Compensation: rollback points failed", e);
                    }
                });
            } else if (!request.paymentMethod().equals(PaymentMethod.CASH)) {
                saved.setStatus(OrderStatus.AWAITING_PAYMENT);
                PaymentServiceClient.PaymentIntentResponse intent = paymentServiceClient
                        .createPaymentIntent(saved.getId(), clientId, saved.getTotalPrice(), "egp");
                saved.setStripePaymentIntentId(intent.paymentIntentId());
                saved.setStripeClientSecret(intent.clientSecret());
                self.updateOrder(saved);
                compensations.push(() -> {
                    try {
                        messagePublisher.publishPaymentRefund(
                                new PaymentRefundRequestedEvent(saved.getId(), clientId, saved.getTotalPrice()),
                                UUID.randomUUID().toString(), UUID.randomUUID().toString());
                    } catch (Exception e) {
                        log.error("Compensation: publishPaymentRefund failed", e);
                    }
                });
            } else {
                log.info("OrderPlacementSaga: CASH payment for order={}, publishing OrderCreated", saved.getId());
                messagePublisher.publishOrderCreated(
                        new OrderCreatedEvent(saved.getId()),
                        UUID.randomUUID().toString(), UUID.randomUUID().toString());
            }

            return orderMapper.toResponse(saved);

        } catch (Exception e) {
            log.error("OrderPlacementSaga: failed for clientId={}. Unwinding {} compensation(s).",
                    clientId, compensations.size(), e);
            while (!compensations.isEmpty()) {
                try {
                    compensations.pop().run();
                } catch (Exception ce) {
                    log.error("Compensation failed", ce);
                }
            }
            throw e;
        }
    }

    // ── Transactional persistence helpers ─────────────────────────────────────

    @Transactional
    public Order saveInitialOrder(Order order) {
        return orderRepository.save(order);
    }

    @Transactional
    public void updateOrder(Order order) {
        orderRepository.save(order);
    }

    @Transactional
    public void markOrderAsFailed(Long orderId) {
        orderRepository.findById(orderId).ifPresent(o -> {
            o.setStatus(OrderStatus.CANCELED);
            orderRepository.save(o);
        });
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Map<Long, MealPriceSnapshot> fetchSnapshots(PlaceOrderRequest request) {
        List<Long> mealIds = request.items().stream()
                .filter(i -> i.mealId() != null)
                .map(OrderItemRequest::mealId)
                .distinct()
                .toList();

        return mealIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> menuClient.getMealById(id)));
    }

    private Map<Long, IngredientDTO> fetchIngredients(PlaceOrderRequest request) {
        boolean hasCustomMeals = request.items().stream().anyMatch(i -> i.mealId() == null);
        if (!hasCustomMeals)
            return Map.of();
        return menuClient.getAllIngredients().stream()
                .collect(Collectors.toMap(IngredientDTO::id, i -> i));
    }

    private List<com.restaurant.order.dto.request.ReserveMealRequest> toStockItems(Order order) {
        return order.getItems().stream()
                .map(item -> {
                    Map<Long, Double> customizations = null;
                    if (item.getCustomizations() != null && !item.getCustomizations().isEmpty()) {
                        customizations = new java.util.HashMap<>();
                        try {
                            Map<String, Object> customMap = item.getCustomizations();
                            
                            // Extract primary
                            if (customMap.containsKey("primary")) {
                                Map<String, Object> primary = (Map<String, Object>) customMap.get("primary");
                                if (primary != null && primary.get("id") != null) {
                                    Long id = Long.valueOf(primary.get("id").toString());
                                    Double grams = primary.containsKey("grams") ? Double.valueOf(primary.get("grams").toString()) : 100.0;
                                    customizations.put(id, grams);
                                }
                            }
                            
                            // Extract additions
                            if (customMap.containsKey("additions")) {
                                List<Map<String, Object>> additions = (List<Map<String, Object>>) customMap.get("additions");
                                if (additions != null) {
                                    for (Map<String, Object> add : additions) {
                                        if (add.get("id") != null && add.get("grams") != null) {
                                            Long id = Long.valueOf(add.get("id").toString());
                                            Double grams = Double.valueOf(add.get("grams").toString());
                                            customizations.merge(id, grams, Double::sum);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Failed to parse customizations for stock reservation", e);
                        }
                    }
                    return new com.restaurant.order.dto.request.ReserveMealRequest(
                            item.getMealId(), item.getQuantity(), customizations);
                })
                .toList();
    }
}

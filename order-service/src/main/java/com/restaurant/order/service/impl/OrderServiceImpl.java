package com.restaurant.order.service.impl;

import com.restaurant.order.client.MenuClient;
import com.restaurant.order.dto.request.OrderItemRequest;
import com.restaurant.order.dto.request.PlaceOrderRequest;
import com.restaurant.order.dto.response.OrderResponse;
import com.restaurant.order.dto.snapshot.MealPriceSnapshot;
import com.restaurant.order.entity.Order;
import com.restaurant.order.entity.OrderItem;
import com.restaurant.order.enums.OrderStatus;
import com.restaurant.order.events.OrderCreatedEvent;
import com.restaurant.order.events.payments.PaymentRequestedEvent;
import com.restaurant.order.events.points.PointRedemptionRequestedEvent;
import com.restaurant.order.events.points.RewardPointsEarnedEvent;
import com.restaurant.order.exception.MenuServiceException;
import com.restaurant.order.exception.OrderNotFoundException;
import com.restaurant.order.exception.PaymentException;
import com.restaurant.order.exception.PointsException;
import com.restaurant.order.mapper.OrderMapper;
import com.restaurant.order.messaging.MessagePublisher;
import com.restaurant.order.repository.OrderRepository;
import com.restaurant.order.service.OrderCalculator;
import com.restaurant.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MessagePublisher messagePublisher;
    private final OrderMapper orderMapper;
    private final OrderCalculator orderCalculator;
    private final MenuClient menuClient;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderResponse placeOrder(PlaceOrderRequest request, Long customerId) {
        log.info("Placing order for customerId: {}", customerId);

        // 1. Validate points
        int pointsToRedeem = request.points() != null ? request.points() : 0;
        int discount = switch (pointsToRedeem){
            case 0 -> 0;
            case 100 -> 10;
            case 200 -> 20;
            case 300 -> 30;
            default -> throw new PointsException(
                    "Invalid redemption points. Allowed values are 100, 200, or 300." // cancel order process
            );
        };

        Order order = Order.builder()
                .customerId(customerId)
                .discount(discount)
                .build();

        // 2. Fetch price snapshots from menu-service and build items
        for (OrderItemRequest itemReq : request.items()) {
            MealPriceSnapshot meal = menuClient.getMealById(itemReq.mealId());
            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .mealId(meal.id())
                    .snapshotName(meal.name())
                    .snapshotPrice(meal.price())
                    .quantity(itemReq.quantity())
                    .build());
        }

        // 3. Calculate total price (with point discount if applicable)
        orderCalculator.calculateTotals(order);

        // 4. Reserve stock — if this fails, nothing is written to DB
        log.info("Reserving stock for order...");
        try {
            menuClient.reserveStock(request.items());
        } catch (Exception e){
            log.error("Failed to reserve stock for order.", e);
            throw new MenuServiceException("Cannot reserve stock for order. The stock may be insufficient");
        }

        try {
            // 5. Persist to DB — status starts as PENDING
            Order savedOrder = orderRepository.save(order);
            log.info("Order saved with id: {} and status PENDING", savedOrder.getId());

            // 6. Trigger next step in saga
            if (pointsToRedeem != 0) {
                log.info("Triggering async point redemption for {} points...", pointsToRedeem);
                publishPointRedemptionRequestedEvent(savedOrder);
            } else {
                savedOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
                orderRepository.save(savedOrder);
                publishPaymentRequestedEvent(savedOrder);
            }

            return orderMapper.toResponse(savedOrder);
        } catch (Exception e) {
            log.error("Failed to complete order placement for customerId: {}. Rolling back stock.", customerId, e);
            try {
                menuClient.rollbackStock(buildItemRequests(order));
            } catch (Exception rollbackEx) {
                log.error("CRITICAL: Failed to rollback stock after order placement failure for customerId: {}", customerId, rollbackEx);
            }
            throw e; // Re-throw the original exception (PointsException, PaymentException, or DB exception)
        }
    }

    @Transactional
    @Override
    public void confirmOrder(Long orderId, Long ticketId) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    try {
                        if (order.getStatus() == OrderStatus.CONFIRMED) {
                            log.info("Order {} already confirmed, skipping.", orderId);
                            return;
                        }
                        order.setStatus(OrderStatus.CONFIRMED);
                        orderRepository.save(order);
                        log.info("Order {} status updated to CONFIRMED (ticket {})", orderId, ticketId);

                        // Reward points: 1 point per 5$ spent
                        if(order.getDiscount() == 0) {
                            int pointsToReward = order.getTotalPrice().intValue() / 5;
                            if (pointsToReward > 0) {
                                messagePublisher.publishRewardPointsEarned(
                                        new RewardPointsEarnedEvent(order.getId(), order.getCustomerId(), pointsToReward)
                                );
                            }
                        }
                    } catch (Exception e) {
                        log.error("MANUAL INTERVENTION REQUIRED: Failed to confirm order {}. Error: {}", orderId, e.getMessage());
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    }
                },
                () -> log.error("MANUAL INTERVENTION REQUIRED: Order not found for confirmation, orderId: {}", orderId)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markPaymentSucceeded(Long orderId) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    try {
                        if (order.getStatus() == OrderStatus.PAID) {
                            log.info("Order {} already marked as PAID, skipping.", orderId);
                            return;
                        }

                        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
                            log.warn("Order {} is in status {}, expected AWAITING_PAYMENT. Ignoring payment success.", orderId, order.getStatus());
                            return;
                        }

                        order.setStatus(OrderStatus.PAID);
                        orderRepository.save(order);
                        log.info("Payment succeeded for order {}. Notifying kitchen...", orderId);
                        publishOrderCreatedEvent(order);
                    } catch (Exception e) {
                        log.error("MANUAL INTERVENTION REQUIRED: Failed to process payment success for order {}. Error: {}", orderId, e.getMessage());
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    }
                },
                () -> log.error("MANUAL INTERVENTION REQUIRED: Order not found for payment success, ID: {}", orderId)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markPaymentFailed(Long orderId, String reason) {
        log.info("Payment failed for order {}, reason: {}. Cancelling order...", orderId, reason);
        cancelOrder(orderId, "Payment failed: " + reason);
    }

    @Transactional
    @Override
    public void markPointRedemptionSucceeded(Long orderId) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    try {
                        if (order.getStatus() == OrderStatus.AWAITING_PAYMENT || order.getStatus() == OrderStatus.PAID) {
                            log.info("Order {} already processed point redemption, skipping.", orderId);
                            return;
                        }

                        if (order.getStatus() != OrderStatus.PENDING) {
                            log.warn("Order {} is in status {}, expected PENDING. Ignoring point redemption success.", orderId, order.getStatus());
                            return;
                        }

                        order.setStatus(OrderStatus.AWAITING_PAYMENT);
                        orderRepository.save(order);
                        log.info("Point redemption succeeded for order {}. Proceeding to payment...", orderId);
                        publishPaymentRequestedEvent(order);
                    } catch (Exception e) {
                        log.error("MANUAL INTERVENTION REQUIRED: Failed to process point redemption success for order {}. Error: {}", orderId, e.getMessage());
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    }
                },
                () -> log.error("MANUAL INTERVENTION REQUIRED: Order not found for point redemption success, ID: {}", orderId)
        );
    }

    @Transactional
    @Override
    public void markPointRedemptionFailed(Long orderId, String reason) {
        log.info("Point redemption failed for order {}, reason: {}. Cancelling order...", orderId, reason);
        cancelOrder(orderId, "Point redemption failed: " + reason);
    }

    @Override
    public OrderResponse retrieveOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderMapper.toResponse(order);
    }

    @Transactional
    @Override
    public void cancelOrder(Long orderId, String reason) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    try {
                        if (order.getStatus() == OrderStatus.CANCELLED) {
                            log.info("Order {} already cancelled, skipping.", orderId);
                            return;
                        }

                        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
                            try {
                                log.info("Rolling back stock for cancelled order {}", orderId);
                                menuClient.rollbackStock(buildItemRequests(order));
                            } catch (Exception e) {
                                log.error("Failed to rollback stock for cancelled order {}", orderId, e);
                            }
                        }
                        order.setStatus(OrderStatus.CANCELLED);
                        orderRepository.save(order);
                        log.info("Order {} cancelled. Reason: {}", orderId, reason);
                    } catch (Exception e) {
                        log.error("MANUAL INTERVENTION REQUIRED: Failed to cancel order {}. Error: {}", orderId, e.getMessage());
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    }
                },
                () -> log.error("MANUAL INTERVENTION REQUIRED: Order not found for cancellation, orderId: {}", orderId)
        );
    }

    @Transactional
    @Override
    public void onTicketReady(Long orderId) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    try {
                        if (order.getStatus() == OrderStatus.READY_FOR_PICKUP) {
                            log.info("Order {} already marked as READY_FOR_PICKUP, skipping.", orderId);
                            return;
                        }
                        order.setStatus(OrderStatus.READY_FOR_PICKUP);
                        orderRepository.save(order);
                    } catch (Exception e) {
                        log.error("MANUAL INTERVENTION REQUIRED: Failed to mark order {} as ready. Error: {}", orderId, e.getMessage());
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    }
                },
                () -> log.error("MANUAL INTERVENTION REQUIRED: Order not found for ticket ready status, ID: {}", orderId)
        );
    }


    // --- Private helpers ---

    private List<OrderItemRequest> buildItemRequests(Order order) {
        return order.getItems().stream()
                .map(item -> new OrderItemRequest(item.getMealId(), item.getQuantity()))
                .toList();
    }

    private void publishOrderCreatedEvent(Order order) {
        try {
            String sagaId = UUID.randomUUID().toString();
            String correlationId = UUID.randomUUID().toString();
            messagePublisher.publishOrderCreated(
                    new OrderCreatedEvent(order.getId()), sagaId, correlationId);
        } catch (Exception e) {
            log.error("Failed to publish order.created event for orderId: {}", order.getId(), e);
            throw new RuntimeException("Order created but failed to notify kitchen service", e);
        }
    }

    private void publishPaymentRequestedEvent(Order order) {
        try {
            String sagaId = UUID.randomUUID().toString();
            String correlationId = UUID.randomUUID().toString();
            messagePublisher.publishPaymentRequested(
                    new PaymentRequestedEvent(order.getId(), order.getCustomerId(), order.getTotalPrice()), sagaId, correlationId);
        } catch (Exception e) {
            log.error("Failed to publish payment.requested event for orderId: {}", order.getId(), e);
            throw new PaymentException("Order placement failed due to payment request failure");
        }
    }

    private void publishPointRedemptionRequestedEvent(Order order) {
        try {
            String sagaId = UUID.randomUUID().toString();
            String correlationId = UUID.randomUUID().toString();
            messagePublisher.publishPointRedemptionRequested(
                    new PointRedemptionRequestedEvent(order.getId(), order.getCustomerId(), order.getDiscount() * 10), sagaId, correlationId);
        } catch (Exception e) {
            log.error("Failed to publish point-redemption.requested event for orderId: {}", order.getId(), e);
            throw new PointsException("Order placement failed due to point redemption request failure");
        }
    }
}

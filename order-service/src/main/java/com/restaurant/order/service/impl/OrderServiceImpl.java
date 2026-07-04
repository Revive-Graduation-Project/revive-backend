package com.restaurant.order.service.impl;

import com.restaurant.order.client.MenuClient;
import com.restaurant.order.client.PaymentServiceClient;
import com.restaurant.order.dto.request.OrderItemRequest;
import com.restaurant.order.dto.request.PlaceOrderRequest;
import com.restaurant.order.dto.response.OrderResponse;
import com.restaurant.order.dto.snapshot.MealPriceSnapshot;
import com.restaurant.order.entity.Order;
import com.restaurant.order.entity.OrderItem;
import com.restaurant.order.enums.OrderStatus;
import com.restaurant.order.enums.PaymentMethod;
import com.restaurant.order.events.OrderCancellationEvent;
import com.restaurant.order.events.OrderCreatedEvent;
import com.restaurant.order.events.payments.PaymentRefundRequestedEvent;
import com.restaurant.order.events.points.PointRedemptionRollbackRequestedEvent;
import com.restaurant.order.events.points.PointRedemptionRequestedEvent;
import com.restaurant.order.events.points.RewardPointsEarnedEvent;
import com.restaurant.order.exception.MenuServiceException;
import com.restaurant.order.exception.OrderNotFoundException;
import com.restaurant.order.exception.PointsException;
import com.restaurant.order.mapper.OrderMapper;
import com.restaurant.order.messaging.MessagePublisher;
import com.restaurant.order.repository.OrderRepository;
import com.restaurant.order.service.OrderCalculator;
import com.restaurant.order.service.OrderService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MessagePublisher messagePublisher;
    private final OrderMapper orderMapper;
    private final OrderCalculator orderCalculator;
    private final MenuClient menuClient;
    private final PaymentServiceClient paymentServiceClient;

    @Autowired
    @Lazy
    private OrderServiceImpl self = this;

    public record CancelResult(OrderStatus previousStatus, Order order) {}

    //  @Transactional is not defined to prevent DB locking during HTTP calls
    @Override
    public OrderResponse placeOrder(PlaceOrderRequest request, Long clientId) {
        log.info("Placing order for customerId: {}", clientId);

        // 1. Validate points
        int pointsToRedeem = request.points() != null ? request.points() : 0;
        int discount = switch (pointsToRedeem) {
            case 0 -> 0;
            case 100 -> 10;
            case 200 -> 20;
            case 300 -> 30;
            default -> throw new PointsException("Invalid redemption points. Allowed values are 100, 200, or 300.");
        };

        Order order = Order.builder()
                .clientId(clientId)
                .discount(discount)
                .paymentMethod(request.paymentMethod())
                .build();

        // 2. Fetch price snapshots one by one (Outside of DB transaction)
        for (OrderItemRequest itemReq : request.items()) {
            try {
                MealPriceSnapshot meal = menuClient.getMealById(itemReq.mealId());
                order.getItems().add(OrderItem.builder()
                        .order(order)
                        .mealId(meal.id())
                        .snapshotName(meal.name())
                        .snapshotPrice(meal.price())
                        .snapshotImageUrl(meal.imageUrl())
                        .quantity(itemReq.quantity())
                        .build());
            } catch (Exception e) {
                log.error("Failed to fetch meal from Menu Service for ID: {}", itemReq.mealId(), e);
                throw new MenuServiceException("Meal not found or Menu Service unavailable for ID: " + itemReq.mealId());
            }
        }

        // 3. Calculate total price
        orderCalculator.calculateTotals(order);

        // 4. Reserve stock via HTTP
        log.info("Reserving stock for order...");
        try {
            menuClient.reserveStock(request.items());
        } catch (Exception e) {
            log.error("Failed to reserve stock for order.", e);
            throw new MenuServiceException("Cannot reserve stock for order. The stock may be insufficient");
        }

        try {
            // 5. Persist to DB using the private transactional method (Protects the DB connection pool)
            Order savedOrder = self.saveInitialOrder(order);
            log.info("Order saved with id: {} and status PENDING", savedOrder.getId());

            // 6. Trigger next step in saga via Network/RabbitMQ
            if (pointsToRedeem != 0) {
                log.info("Triggering async point redemption for {} points...", pointsToRedeem);
                publishPointRedemptionRequestedEvent(savedOrder);
            } else {
                if (!request.paymentMethod().equals(PaymentMethod.CASH)) {
                    savedOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
                    createPaymentIntentSync(savedOrder); // HTTP call
                    self.updateOrderInDb(savedOrder);         // Save intent IDs
                } else {
                    log.info("Payment method is CASH for order {}, skipping payment service", savedOrder.getId());
                    publishOrderCreatedEvent(savedOrder);
                }
            }
            return orderMapper.toResponse(savedOrder);

        } catch (Exception e) {
            log.error("Failed to complete order placement for clientId: {}. Rolling back stock.", clientId, e);
            try {
                menuClient.rollbackStock(buildItemRequests(order));
                if (order.getId() != null) {
                    self.markOrderAsFailed(order.getId());
                }
            } catch (Exception rollbackEx) {
                log.error("CRITICAL: Failed to rollback stock after order placement failure", rollbackEx);
            }
            throw e;
        }
    }

    @Transactional
    @Override
    public void confirmOrder(Long orderId, Long ticketId) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    try {
                        if (order.getStatus() == OrderStatus.CONFIRMED) {
                            return;
                        }
                        order.setStatus(OrderStatus.CONFIRMED);
                        orderRepository.save(order);
                        log.info("Order {} status updated to CONFIRMED (ticket {})", orderId, ticketId);

                        if (order.getDiscount() == 0) {
                            int pointsToReward = order.getTotalPrice().intValue() / 5;
                            if (pointsToReward > 0) {
                                messagePublisher.publishRewardPointsEarned(
                                        new RewardPointsEarnedEvent(order.getClientId(), pointsToReward, order.getId())
                                );
                            }
                        }
                    } catch (Exception e) {
                        log.error("MANUAL INTERVENTION REQUIRED: Failed to confirm order {}. Error: {}", orderId, e.getMessage());
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    }
                },
                () -> log.error("Order not found for confirmation, orderId: {}", orderId)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markPaymentSucceeded(Long orderId) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {

                    if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) return;

                    order.setStatus(OrderStatus.PAID);
                    orderRepository.save(order);
                    publishOrderCreatedEvent(order);
                },
                () -> log.error("Order not found for payment success, ID: {}", orderId)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markPaymentFailed(Long orderId, String reason) {
        log.info("Payment failed for order {}, reason: {}. Cancelling order...", orderId, reason);
        cancelOrder(orderId, "Payment failed: " + reason);
    }

    @Override
    public void markPointRedemptionSucceeded(Long orderId) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    if (order.getStatus() != OrderStatus.PENDING) return;

                    if (!order.getPaymentMethod().equals(PaymentMethod.CASH)) {
                        order.setStatus(OrderStatus.AWAITING_PAYMENT);
                        createPaymentIntentSync(order);
                        self.updateOrderInDb(order); // Save intent IDs
                    } else {
                        publishOrderCreatedEvent(order);
                    }
                },
                () -> log.error("Order not found for point redemption success, ID: {}", orderId)
        );
    }

    @Transactional
    @Override
    public void markPointRedemptionFailed(Long orderId, String reason) {
        log.info("Point redemption failed for order {}, reason: {}. Cancelling...", orderId, reason);
        cancelOrder(orderId, "Point redemption failed: " + reason);
    }

    @Override
    public void processTicketCancellationSuccess(Long orderId) {
        Order order = self.processTicketCancellationSuccessInDb(orderId);
        if (order != null) {
            log.info("Kitchen confirmed cancellation for order ID: {}", orderId);
            // Trigger refund logic for successful late cancellations, explicitly treating payment as succeeded
            executeCompensatingTransactions(order, true);
        } else {
            log.error("Order not found for ticket cancellation success, ID: {}", orderId);
        }
    }

    @Transactional
    public Order processTicketCancellationSuccessInDb(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setStatus(OrderStatus.CANCELED);
            orderRepository.save(order);
        }
        return order;
    }

    // Fix: Added @Transactional and reason parameter
    @Transactional
    @Override
    public void processTicketCancellationFailure(Long orderId, String reason) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    order.setStatus(OrderStatus.CONFIRMED);
                    orderRepository.save(order);
                    log.warn("Kitchen refused cancellation for order {}. Reverted to CONFIRMED. Reason: {}", orderId, reason);
                },
                () -> log.error("Order not found for ticket cancellation failure, ID: {}", orderId)
        );
    }

    @Override
    public OrderResponse retrieveOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderMapper.toResponse(order);
    }

    @Override
    public void cancelOrder(Long orderId, @Nullable String reason) {
        CancelResult result = self.cancelOrderInDb(orderId);
        Order order = result.order();
        switch (result.previousStatus()) {
            case PENDING, AWAITING_PAYMENT, PAID -> {
                boolean paymentSucceeded = result.previousStatus() == OrderStatus.PAID;
                log.info("Order {} cancelled. Executing compensations...", orderId);
                executeCompensatingTransactions(order, paymentSucceeded);
            }
            case CONFIRMED -> {
                messagePublisher.publishOrderCanceled(
                        new OrderCancellationEvent(orderId),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString()
                );
            }
            default -> {}
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public CancelResult cancelOrderInDb(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        
        OrderStatus previousStatus = order.getStatus();
        switch (previousStatus) {
            case PENDING, AWAITING_PAYMENT, PAID -> {
                order.setStatus(OrderStatus.CANCELED);
                orderRepository.save(order);
            }
            case CONFIRMED -> {
                order.setStatus(OrderStatus.CANCELLATION_PENDING);
                orderRepository.save(order);
            }
            case PREPARING, READY -> throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel order in status: " + previousStatus);
            case CANCELED, CANCELLATION_PENDING -> log.info("Order {} already cancelled", orderId);
        }
        return new CancelResult(previousStatus, order);
    }

    @Transactional
    @Override
    public void onTicketReady(Long orderId) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    order.setStatus(OrderStatus.READY);
                    orderRepository.save(order);
                },
                () -> log.error("Order not found for ticket ready status, ID: {}", orderId)
        );
    }

    @Transactional
    @Override
    public void onTicketStarted(Long orderId, Long ticketId) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    order.setStatus(OrderStatus.PREPARING);
                    orderRepository.save(order);
                },
                () -> log.error("Order not found for ticket started status, ID: {}", orderId)
        );
    }

    @Override
    public List<OrderResponse> getClientOrderHistory(Long clientId) {
        return orderRepository.findDistinctByClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    // --- Private helpers ---

    @Transactional
    public Order saveInitialOrder(Order order) {
        return orderRepository.save(order);
    }

    @Transactional
    public void updateOrderInDb(Order order) {
        orderRepository.save(order);
    }

    @Transactional
    public void markOrderAsFailed(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(OrderStatus.CANCELED);
            orderRepository.save(order);
        });
    }

    private void executeCompensatingTransactions(Order order, boolean paymentSucceeded) {
        try {
            menuClient.rollbackStock(buildItemRequests(order));
        } catch (Exception e) {
            log.error("Failed to rollback stock for cancelled order {}", order.getId(), e);
        }

        if (paymentSucceeded) {
            publishPaymentRefundEvent(order);
        }

        if (order.getDiscount() > 0) {
            publishPointRedemptionRollbackEvent(order);
        }
    }

    private List<OrderItemRequest> buildItemRequests(Order order) {
        return order.getItems().stream()
                .map(item -> new OrderItemRequest(item.getMealId(), item.getQuantity()))
                .toList();
    }

    private void publishOrderCreatedEvent(Order order) {
        try {
            messagePublisher.publishOrderCreated(
                    new OrderCreatedEvent(order.getId()), UUID.randomUUID().toString(), UUID.randomUUID().toString());
        } catch (Exception e) {
            log.error("Failed to publish order.created event for orderId: {}", order.getId(), e);
            throw new RuntimeException("Order created but failed to notify kitchen service", e);
        }
    }

    private void createPaymentIntentSync(Order order) {
        PaymentServiceClient.PaymentIntentResponse response = paymentServiceClient.createPaymentIntent(
                order.getId(), order.getClientId(), order.getTotalPrice(), "egp");
        order.setStripePaymentIntentId(response.paymentIntentId());
        order.setStripeClientSecret(response.clientSecret());
    }

    private void publishPointRedemptionRequestedEvent(Order order) {
        messagePublisher.publishPointRedemptionRequested(
                new PointRedemptionRequestedEvent(order.getId(), order.getClientId(), order.getDiscount() * 10),
                UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    private void publishPointRedemptionRollbackEvent(Order order) {
        messagePublisher.publishPointRedemptionRollback(
                new PointRedemptionRollbackRequestedEvent(order.getId(), order.getClientId(), order.getDiscount() * 10),
                UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    private void publishPaymentRefundEvent(Order order) {
        messagePublisher.publishPaymentRefund(
                new PaymentRefundRequestedEvent(order.getId(), order.getClientId(), order.getTotalPrice()),
                UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }
}
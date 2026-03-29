package com.restaurant.order.service.impl;

import com.restaurant.order.client.InventoryClient;
import com.restaurant.order.client.MenuClient;
import com.restaurant.order.dto.request.PlaceOrderRequest;
import com.restaurant.order.dto.response.OrderResponse;
import com.restaurant.order.dto.snapshot.MealRecipeIngredientSnapshot;
import com.restaurant.order.dto.snapshot.MealSnapshot;
import com.restaurant.order.entity.CustomOrderItem;
import com.restaurant.order.entity.Order;
import com.restaurant.order.entity.OrderItem;
import com.restaurant.order.enums.OrderStatus;
import com.restaurant.order.events.OrderCreatedEvent;
import com.restaurant.order.mapper.OrderMapper;
import com.restaurant.order.messaging.MessagePublisher;
import com.restaurant.order.repository.OrderRepository;
import com.restaurant.order.service.OrderCalculator;
import com.restaurant.order.service.OrderService;
import com.restaurant.order.service.ProductProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MessagePublisher messagePublisher;
    private final OrderMapper orderMapper;
    private final OrderCalculator orderCalculator;
    private final List<ProductProcessor<?>> processors;
    private final InventoryClient inventoryClient;
    private final MenuClient menuClient;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderResponse placeOrder(PlaceOrderRequest request, Long customerId) {
        log.info("Placing order for customerId: {}", customerId);

        Order order = Order.builder()
                .customerId(customerId)
                .build();

        // 1. Use Strategy Pattern to process items
        processItems(order, request);

        // 2. Calculate totals
        orderCalculator.calculateTotals(order);

        // 3. Reserve inventory (Pre-check before saving/publishing)
        log.info("Reserving inventory for new order...");
        Map<UUID, Double> requiredIngredients = aggregateIngredients(order);
        inventoryClient.reserve(requiredIngredients);

        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with id: {} and status PENDING", savedOrder.getId());

        publishOrderCreatedEvent(savedOrder);

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional
    @Override
    public void confirmOrder(Long orderId, Long ticketId) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    order.setStatus(OrderStatus.CONFIRMED);
                    orderRepository.save(order);
                    log.info("Order {} status updated to CONFIRMED (ticket {})", orderId, ticketId);
                },
                () -> log.error("Order not found for confirmation, orderId: {}", orderId)
        );
    }

    @Transactional
    @Override
    public void cancelOrder(Long orderId, String reason) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    // Rollback if already reserved
                    if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CONFIRMED) {
                        try {
                            log.info("Rolling back inventory for cancelled order {}", orderId);
                            Map<UUID, Double> requiredIngredients = aggregateIngredients(order);
                            inventoryClient.rollback(requiredIngredients);
                        } catch (Exception e) {
                            log.error("Failed to rollback inventory for cancelled order {}", orderId, e);
                        }
                    }
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);
                    log.info("Order {} cancelled. Reason: {}", orderId, reason);
                },
                () -> log.error("Order not found for cancellation, orderId: {}", orderId)
        );
    }

    @Transactional
    @Override
    public void onTicketReady(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        log.info("Ticket ready for order {}. Committing inventory...", orderId);

        try {
            Map<UUID, Double> requiredIngredients = aggregateIngredients(order);
            inventoryClient.commit(requiredIngredients);
            
            order.setStatus(OrderStatus.READY_FOR_PICKUP);
            orderRepository.save(order);
            log.info("Order {} is now READY_FOR_PICKUP and inventory committed", orderId);
        } catch (Exception e) {
            log.error("Failed to commit inventory for order {}. Manual intervention may be required.", orderId, e);
            throw e;
        }
    }

    private Map<UUID, Double> aggregateIngredients(Order order) {
        Map<UUID, Double> aggregation = new HashMap<>();

        // 1. From standard Meals
        for (OrderItem item : order.getItems()) {
            MealSnapshot meal = menuClient.getMealById(item.getMealId());
            if (meal.ingredients() != null) {
                for (MealRecipeIngredientSnapshot ing : meal.ingredients()) {
                    UUID id = ing.ingredientId();
                    Double grams = ing.quantityGrams() * item.getQuantity();
                    aggregation.put(id, aggregation.getOrDefault(id, 0.0) + grams);
                }
            }
        }

        // 2. From Custom Items
        for (CustomOrderItem customItem : order.getCustomItems()) {
            UUID id = customItem.getIngredientId();
            Double grams = customItem.getQuantityGrams();
            aggregation.put(id, aggregation.getOrDefault(id, 0.0) + grams);
        }

        return aggregation;
    }

    @SuppressWarnings("unchecked")
    private void processItems(Order order, PlaceOrderRequest request) {
        if (request.items() != null) {
            ProductProcessor<Object> mealProcessor = (ProductProcessor<Object>) findProcessor(request.items().get(0));
            request.items().forEach(item -> mealProcessor.process(order, item));
        }

        if (request.customItems() != null) {
            ProductProcessor<Object> customProcessor = (ProductProcessor<Object>) findProcessor(request.customItems().get(0));
            request.customItems().forEach(item -> customProcessor.process(order, item));
        }
    }

    private ProductProcessor<?> findProcessor(Object requestItem) {
        return processors.stream()
                .filter(p -> p.supports(requestItem))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No processor found for item type: " + requestItem.getClass()));
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
}

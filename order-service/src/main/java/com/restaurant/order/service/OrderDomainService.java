package com.restaurant.order.service;

import com.restaurant.order.dto.IngredientDTO;
import com.restaurant.order.dto.request.OrderItemRequest;
import com.restaurant.order.dto.request.PlaceOrderRequest;
import com.restaurant.order.dto.snapshot.MealPriceSnapshot;
import com.restaurant.order.entity.Order;
import com.restaurant.order.entity.OrderItem;
import com.restaurant.order.exception.MenuServiceException;
import com.restaurant.order.exception.PointsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Pure, stateless deep module for core order domain logic.
 * No HTTP calls, no database access — all external data is provided as arguments.
 * Given a request and pre-fetched snapshots, returns a fully constructed, unpersisted Order.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderDomainService {

    private final OrderCalculator orderCalculator;

    /**
     * Validates the request, applies discount logic, builds OrderItems from snapshots,
     * calculates totals and returns a fully constructed, unpersisted Order.
     *
     * @param request       the raw placement request
     * @param clientId      the authenticated client's ID
     * @param snapshots     meal price snapshots keyed by mealId (pre-fetched by saga)
     * @param ingredientMap ingredient data keyed by ingredientId (pre-fetched for custom meals)
     * @throws PointsException       if an invalid point redemption value is supplied
     * @throws MenuServiceException  if customization data is invalid or missing
     */
    public Order buildOrder(
            PlaceOrderRequest request,
            Long clientId,
            Map<Long, MealPriceSnapshot> snapshots,
            Map<Long, IngredientDTO> ingredientMap
    ) {
        int pointsToRedeem = request.points() != null ? request.points() : 0;
        int discount = resolveDiscount(pointsToRedeem);

        Order order = Order.builder()
                .clientId(clientId)
                .discount(discount)
                .paymentMethod(request.paymentMethod())
                .build();

        for (OrderItemRequest itemReq : request.items()) {
            OrderItem item = buildItem(order, itemReq, snapshots, ingredientMap);
            order.getItems().add(item);
        }

        orderCalculator.calculateTotals(order);
        return order;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private int resolveDiscount(int points) {
        return switch (points) {
            case 0   -> 0;
            case 100 -> 10;
            case 200 -> 20;
            case 300 -> 30;
            default  -> throw new PointsException(
                    "Invalid redemption points. Allowed values are 100, 200, or 300.");
        };
    }

    private OrderItem buildItem(
            Order order,
            OrderItemRequest itemReq,
            Map<Long, MealPriceSnapshot> snapshots,
            Map<Long, IngredientDTO> ingredientMap
    ) {
        if (itemReq.mealId() != null) {
            MealPriceSnapshot meal = snapshots.get(itemReq.mealId());
            if (meal == null) {
                throw new MenuServiceException(
                        "No price snapshot found for meal ID: " + itemReq.mealId());
            }
            return OrderItem.builder()
                    .order(order)
                    .mealId(meal.id())
                    .snapshotName(meal.name())
                    .snapshotPrice(meal.price())
                    .snapshotImageUrl(meal.imageUrl())
                    .quantity(itemReq.quantity())
                    .build();
        } else {
            return buildCustomItem(order, itemReq, ingredientMap);
        }
    }

    private OrderItem buildCustomItem(
            Order order,
            OrderItemRequest itemReq,
            Map<Long, IngredientDTO> ingredientMap
    ) {
        if (itemReq.customizations() == null || itemReq.customizations().isEmpty()) {
            throw new MenuServiceException("Customizations cannot be empty for custom meal");
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        String name = "Custom Meal";

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> customizations = itemReq.customizations();

            @SuppressWarnings("unchecked")
            Map<String, Object> primary = (Map<String, Object>) customizations.get("primary");
            if (primary != null && primary.get("id") != null) {
                Long primaryId = Long.valueOf(primary.get("id").toString());
                IngredientDTO ing = ingredientMap.get(primaryId);
                if (ing != null) {
                    totalPrice = totalPrice.add(BigDecimal.valueOf(ing.price()));
                    name = "Custom " + ing.name() + " Meal";
                }
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> additions =
                    (List<Map<String, Object>>) customizations.get("additions");
            if (additions != null) {
                for (Map<String, Object> add : additions) {
                    if (add.get("id") == null || add.get("grams") == null) continue;
                    Long addId = Long.valueOf(add.get("id").toString());
                    Double grams = Double.valueOf(add.get("grams").toString());
                    IngredientDTO ing = ingredientMap.get(addId);
                    if (ing != null) {
                        totalPrice = totalPrice.add(
                                BigDecimal.valueOf(ing.price())
                                          .multiply(BigDecimal.valueOf(grams)));
                    }
                }
            }
        } catch (MenuServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse customizations for price calculation", e);
            throw new MenuServiceException("Invalid customization data");
        }

        return OrderItem.builder()
                .order(order)
                .mealId(null)
                .customizations(itemReq.customizations())
                .snapshotName(name)
                .snapshotPrice(totalPrice)
                .snapshotImageUrl(null)
                .quantity(itemReq.quantity())
                .build();
    }
}

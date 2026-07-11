package com.restaurant.order.service;

import com.restaurant.order.dto.IngredientDTO;
import com.restaurant.order.dto.request.OrderItemRequest;
import com.restaurant.order.dto.request.PlaceOrderRequest;
import com.restaurant.order.dto.snapshot.MealPriceSnapshot;
import com.restaurant.order.entity.Order;
import com.restaurant.order.enums.PaymentMethod;
import com.restaurant.order.exception.MenuServiceException;
import com.restaurant.order.exception.PointsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OrderDomainService — zero mocks, all inputs are plain Java objects.
 * Seam: OrderDomainService.buildOrder(request, clientId, snapshots, ingredientMap)
 */
class OrderDomainServiceTest {

    private OrderDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new OrderDomainService(new OrderCalculator());
    }

    // ── Points validation ──────────────────────────────────────────────────────

    @Test
    void buildOrder_withZeroPoints_appliesNoDiscount() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(1L, null, 2)),
                0,
                PaymentMethod.CASH
        );
        Map<Long, MealPriceSnapshot> snapshots = Map.of(
                1L, new MealPriceSnapshot(1L, "Burger", BigDecimal.valueOf(10.00), null)
        );

        Order order = domainService.buildOrder(request, 42L, snapshots, Map.of());

        assertEquals(0, order.getDiscount());
        assertEquals(new BigDecimal("20.00"), order.getTotalPrice());
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 200, 300})
    void buildOrder_withValidPoints_appliesCorrectDiscount(int points) {
        int expectedDiscountPct = points / 10;
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(1L, null, 1)),
                points,
                PaymentMethod.CREDIT_CARD
        );
        Map<Long, MealPriceSnapshot> snapshots = Map.of(
                1L, new MealPriceSnapshot(1L, "Steak", BigDecimal.valueOf(100.00), null)
        );

        Order order = domainService.buildOrder(request, 42L, snapshots, Map.of());

        assertEquals(expectedDiscountPct, order.getDiscount());
        BigDecimal expectedTotal = BigDecimal.valueOf(100.00)
                .subtract(BigDecimal.valueOf(100.00).multiply(BigDecimal.valueOf(expectedDiscountPct).divide(BigDecimal.valueOf(100))));
        assertEquals(expectedTotal.setScale(2), order.getTotalPrice());
    }

    @ParameterizedTest
    @ValueSource(ints = {50, 150, 250, 999})
    void buildOrder_withInvalidPoints_throwsPointsException(int points) {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(1L, null, 1)),
                points,
                PaymentMethod.CREDIT_CARD
        );
        assertThrows(PointsException.class,
                () -> domainService.buildOrder(request, 42L, Map.of(), Map.of()));
    }

    // ── Order construction ─────────────────────────────────────────────────────

    @Test
    void buildOrder_populatesClientIdAndPaymentMethod() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(1L, null, 1)),
                0,
                PaymentMethod.CASH
        );
        Map<Long, MealPriceSnapshot> snapshots = Map.of(
                1L, new MealPriceSnapshot(1L, "Pizza", BigDecimal.TEN, null)
        );

        Order order = domainService.buildOrder(request, 99L, snapshots, Map.of());

        assertEquals(99L, order.getClientId());
        assertEquals(PaymentMethod.CASH, order.getPaymentMethod());
    }

    @Test
    void buildOrder_snapshotsMealNameAndPrice_ontoOrderItem() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(7L, null, 3)),
                0,
                PaymentMethod.CASH
        );
        Map<Long, MealPriceSnapshot> snapshots = Map.of(
                7L, new MealPriceSnapshot(7L, "Grilled Chicken", BigDecimal.valueOf(25.00), "http://img")
        );

        Order order = domainService.buildOrder(request, 1L, snapshots, Map.of());

        assertEquals(1, order.getItems().size());
        var item = order.getItems().get(0);
        assertEquals(7L, item.getMealId());
        assertEquals("Grilled Chicken", item.getSnapshotName());
        assertEquals(BigDecimal.valueOf(25.00), item.getSnapshotPrice());
        assertEquals("http://img", item.getSnapshotImageUrl());
        assertEquals(3, item.getQuantity());
    }

    // ── Custom meal pricing ────────────────────────────────────────────────────

    @Test
    void buildOrder_customMeal_calculatesFromIngredients() {
        IngredientDTO chicken = new IngredientDTO(1L, "Chicken", "primary", 100.0, 50.0);
        IngredientDTO sauce   = new IngredientDTO(2L, "Sauce",   "addition", 500.0, 0.10);

        Map<String, Object> customizations = Map.of(
                "primary",   Map.of("id", 1),
                "additions", List.of(Map.of("id", 2, "grams", 100.0))
        );

        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(null, customizations, 1)),
                0,
                PaymentMethod.CASH
        );

        Order order = domainService.buildOrder(request, 1L, Map.of(),
                Map.of(1L, chicken, 2L, sauce));

        // 50.0 (chicken) + 0.10 * 100 (sauce grams) = 60.00
        assertEquals(new BigDecimal("60.00"), order.getTotalPrice());
    }

    @Test
    void buildOrder_customMealWithEmptyCustomizations_throwsMenuServiceException() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(null, Map.of(), 1)),
                0,
                PaymentMethod.CASH
        );
        assertThrows(MenuServiceException.class,
                () -> domainService.buildOrder(request, 1L, Map.of(), Map.of()));
    }
}

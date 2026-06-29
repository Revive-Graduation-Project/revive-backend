package com.restaurant.order.controller;

import com.restaurant.order.dto.request.PlaceOrderRequest;
import com.restaurant.order.dto.response.OrderResponse;
import com.restaurant.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Number id = jwt.getClaim("id");
        Long clientId = id.longValue();

        OrderResponse response = orderService.placeOrder(request, clientId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
       OrderResponse order = orderService.retrieveOrder(orderId);
       return ResponseEntity.ok(order);
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Jwt jwt) {

        Number id = jwt.getClaim("id");
        Long clientId = id.longValue();

        // Check if order belongs to the authenticated client
        OrderResponse order = orderService.retrieveOrder(orderId);
        if (!order.clientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to authenticated client");
        }

        orderService.cancelOrder(orderId , null);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/client/history")
    public ResponseEntity<List<OrderResponse>> getClientOrderHistory(
            @AuthenticationPrincipal Jwt jwt) {

        Number id = jwt.getClaim("id");
        Long clientId = id.longValue();

        List<OrderResponse> orderHistory = orderService.getClientOrderHistory(clientId);
        return ResponseEntity.ok(orderHistory);
    }
}

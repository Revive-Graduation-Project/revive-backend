package com.restaurant.order.controller;

import com.restaurant.order.dto.response.OrderResponse;
import com.restaurant.order.enums.OrderStatus;
import com.restaurant.order.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping("/all")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        
        Page<OrderResponse> orders = adminOrderService.getAllOrders(PageRequest.of(page, size), status);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = adminOrderService.getDailyMetrics();
        return ResponseEntity.ok(metrics);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        
        OrderStatus newStatus = OrderStatus.valueOf(body.get("status").toUpperCase());
        adminOrderService.updateOrderStatus(id, newStatus);
        return ResponseEntity.ok().build();
    }
}

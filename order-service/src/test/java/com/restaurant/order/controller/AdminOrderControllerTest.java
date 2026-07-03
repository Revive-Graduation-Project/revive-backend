package com.restaurant.order.controller;

import com.restaurant.order.dto.response.OrderResponse;
import com.restaurant.order.enums.OrderStatus;
import com.restaurant.order.service.AdminOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminOrderService adminOrderService;

    @Test
    void getAllOrders_ReturnsPaginatedOrders() throws Exception {
        OrderResponse order1 = new OrderResponse(1L, 100L, "John Doe", OrderStatus.PENDING, BigDecimal.TEN, 0, LocalDateTime.now(), null, null, List.of());
        Page<OrderResponse> page = new PageImpl<>(List.of(order1), PageRequest.of(0, 10), 1);

        when(adminOrderService.getAllOrders(any(), eq("PENDING"))).thenReturn(page);

        mockMvc.perform(get("/api/orders/admin/all")
                .param("page", "0")
                .param("size", "10")
                .param("status", "PENDING")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));

        verify(adminOrderService).getAllOrders(PageRequest.of(0, 10), "PENDING");
    }

    @Test
    void getMetrics_ReturnsMetricsMap() throws Exception {
        Map<String, Object> metrics = Map.of(
                "totalOrders", 150,
                "totalSales", 5000,
                "preparing", 5,
                "completed", 100
        );

        when(adminOrderService.getDailyMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/orders/admin/metrics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(150))
                .andExpect(jsonPath("$.preparing").value(5));

        verify(adminOrderService).getDailyMetrics();
    }

    @Test
    void updateOrderStatus_UpdatesAndReturnsStatus() throws Exception {
        mockMvc.perform(patch("/api/orders/admin/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"PREPARING\"}"))
                .andExpect(status().isOk());

        verify(adminOrderService).updateOrderStatus(1L, OrderStatus.PREPARING);
    }
}

package com.restaurant.order.mapper;

import com.restaurant.order.dto.response.CustomOrderItemResponse;
import com.restaurant.order.dto.response.OrderItemResponse;
import com.restaurant.order.dto.response.OrderResponse;
import com.restaurant.order.entity.CustomOrderItem;
import com.restaurant.order.entity.Order;
import com.restaurant.order.entity.OrderItem;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderItemResponse toItemResponse(OrderItem item);

    CustomOrderItemResponse toCustomItemResponse(CustomOrderItem item);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> items);

    List<CustomOrderItemResponse> toCustomItemResponseList(List<CustomOrderItem> items);

    default OrderResponse toResponse(Order order) {
        if (order == null) return null;

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getTotalCalories(),
                order.getTotalProtein(),
                order.getTotalCarbs(),
                order.getTotalFats(),
                order.getCreatedAt(),
                toItemResponseList(order.getItems()),
                toCustomItemResponseList(order.getCustomItems())
        );
    }
}

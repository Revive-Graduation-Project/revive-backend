package com.restaurant.kitchen.dto;

import com.restaurant.kitchen.enums.TicketStatus;

import java.time.LocalDateTime;

public record KitchenTicketDTO(
        Long orderId,
        TicketStatus status ,
        String chefDisplayName,
        LocalDateTime createdAt
) {
}

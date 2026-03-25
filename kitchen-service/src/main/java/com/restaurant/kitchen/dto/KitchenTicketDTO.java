package com.restaurant.kitchen.dto;

import com.restaurant.kitchen.enums.TicketStatus;

import java.time.LocalDateTime;

public record KitchenTicketDTO(
        Long id,
        Long orderId,
        TicketStatus status ,
        Long assignedChefId,
        LocalDateTime createdAt
) {}

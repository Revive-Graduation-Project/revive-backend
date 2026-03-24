package com.restaurant.kitchen.events.ticketEvents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketCreatedEvent {
    private Long ticketId;
    private Long orderId;
    private Long assignedChefId;
}

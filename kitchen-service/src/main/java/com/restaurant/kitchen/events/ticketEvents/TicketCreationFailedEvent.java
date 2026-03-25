package com.restaurant.kitchen.events.ticketEvents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketCreationFailedEvent {
    private Long id; // orderId
    private String reason;
}

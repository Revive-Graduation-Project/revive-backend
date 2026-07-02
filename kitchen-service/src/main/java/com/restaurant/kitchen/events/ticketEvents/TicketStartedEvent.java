package com.restaurant.kitchen.events.ticketEvents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketStartedEvent {
    private Long id; // orderId
    private Long ticketId;
}

package com.restaurant.order.events.tickets;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketCreatedEvent {
    private Long id;       // orderId
    private Long ticketId; // kitchen ticket id
}

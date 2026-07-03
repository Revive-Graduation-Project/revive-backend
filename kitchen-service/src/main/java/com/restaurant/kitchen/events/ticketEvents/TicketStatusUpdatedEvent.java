package com.restaurant.kitchen.events.ticketEvents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.restaurant.kitchen.enums.TicketStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatusUpdatedEvent {
    private Long ticketId;
    private Long orderId;
    private TicketStatus status; 
}

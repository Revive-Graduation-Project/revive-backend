package com.restaurant.order.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatusUpdatedEvent {
    private Long ticketId;
    private Long orderId;
    private String status; // QUEUED, PREPARING, READY, DONE, CANCELED
}

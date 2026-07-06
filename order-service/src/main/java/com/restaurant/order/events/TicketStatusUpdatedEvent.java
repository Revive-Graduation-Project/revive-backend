package com.restaurant.order.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.restaurant.order.enums.TicketStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatusUpdatedEvent {
    private Long ticketId;
    private Long orderId;
    private TicketStatus status;
}

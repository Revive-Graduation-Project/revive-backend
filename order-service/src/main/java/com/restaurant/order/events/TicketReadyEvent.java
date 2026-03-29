package com.restaurant.order.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketReadyEvent {
    private Long id;       // orderId
    private Long ticketId;
}

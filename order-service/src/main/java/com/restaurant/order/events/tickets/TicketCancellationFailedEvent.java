package com.restaurant.order.events.tickets;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketCancellationFailedEvent {
    private Long id;
    private String message;
}

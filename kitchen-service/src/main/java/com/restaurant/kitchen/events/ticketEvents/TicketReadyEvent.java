package com.restaurant.kitchen.events.ticketEvents;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketReadyEvent {
  private Long ticketId;
  private Long orderId;
  private Long assignedChefId;
}

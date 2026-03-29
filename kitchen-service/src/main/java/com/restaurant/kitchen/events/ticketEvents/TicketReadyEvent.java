package com.restaurant.kitchen.events.ticketEvents;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketReadyEvent {
  private Long id; //order with this id his ticket in kitchen is set to ready
  private Long ticketId;
}

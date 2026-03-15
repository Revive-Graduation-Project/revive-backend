package com.restaurant.kitchen.events;

import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketReadyEvent {
  private Long orderId;
  private Long assignedChefId;
}

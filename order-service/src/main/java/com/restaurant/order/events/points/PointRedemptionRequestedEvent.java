package com.restaurant.order.events.points;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointRedemptionRequestedEvent {
    private Long id; // orderId
    private Long customerId;
    private Integer points;
}

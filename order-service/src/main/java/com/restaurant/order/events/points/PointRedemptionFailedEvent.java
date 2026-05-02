package com.restaurant.order.events.points;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointRedemptionFailedEvent {
    private Long id; // orderId
    private String reason;
}

package com.restaurant.order.events.points;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointRedemptionSucceededEvent {
    private Long id; // orderId
}

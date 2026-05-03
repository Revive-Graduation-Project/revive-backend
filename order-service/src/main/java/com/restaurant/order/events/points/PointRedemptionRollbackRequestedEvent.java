package com.restaurant.order.events.points;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointRedemptionRollbackRequestedEvent {
    private Long orderId;
    private Long clientId;
    private Integer pointsToRollback;
}

package com.restaurant.order.events.points;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardPointsEarnedEvent {
    private Long clientId;
    private Integer pointsEarned;
    private Long orderId;
}

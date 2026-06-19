package com.restaurant.client.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsEarnedEvent {
    private Long clientId;
    private Integer pointsEarned;
    private Long orderId;
}

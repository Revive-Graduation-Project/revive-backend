package com.restaurant.client.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointRedemptionRequestedEvent {
    private Long clientId;
    private Integer pointsToRedeem;
    private Long orderId;
}

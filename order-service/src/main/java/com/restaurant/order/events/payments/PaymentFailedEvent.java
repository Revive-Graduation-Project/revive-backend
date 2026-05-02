package com.restaurant.order.events.payments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {
    private Long id; // orderId
    private String reason;
}

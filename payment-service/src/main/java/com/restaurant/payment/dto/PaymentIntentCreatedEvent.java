package com.restaurant.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentCreatedEvent {
    private Long orderId;
    private String clientSecret;
    private String paymentIntentId;
}

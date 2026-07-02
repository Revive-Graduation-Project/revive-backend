package com.restaurant.kitchen.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCanceledEvent {
    private Long id; // orderId
}

package com.restaurant.kitchen.events.chefProfileEvents;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCreationFailedEvent {
    private Long id;
    private String reason;
}

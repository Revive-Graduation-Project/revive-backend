package com.restaurant.kitchen.events.chefProfileEvents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCreatedEvent {
    private Long id; //auth user id
}

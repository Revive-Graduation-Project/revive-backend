package com.restaurant.kitchen.events;

import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCreatedEvent {
    private Long chefId;
    private Long authUserId;
    private String displayName;
    private Station station;
    private ChefStatus status;
}

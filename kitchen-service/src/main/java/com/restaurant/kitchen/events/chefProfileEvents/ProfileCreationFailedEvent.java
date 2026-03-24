package com.restaurant.kitchen.events.chefProfileEvents;

import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCreationFailedEvent {
    private Long authUserId;
    private String role;
    private String displayName;
    private Station station;
    private ChefStatus status;

    private String reason;
}

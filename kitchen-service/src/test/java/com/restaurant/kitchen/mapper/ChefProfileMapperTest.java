package com.restaurant.kitchen.mapper;

import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import com.restaurant.kitchen.events.chefProfileEvents.ProfileCreatedEvent;
import com.restaurant.kitchen.events.chefProfileEvents.ProfileCreationFailedEvent;
import com.restaurant.kitchen.events.UserCreatedEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChefProfileMapperTest {

    // Real mapper — not a mock!
    private final ChefProfileMapper mapper = Mappers.getMapper(ChefProfileMapper.class);

    @Test
    void shouldMapChefProfileToProfileCreatedEvent() {
        // Arrange
        ChefProfile chef = ChefProfile.builder()
                .id(1L)
                .authUserId(10L)
                .displayName("John")
                .station(Station.GRILL)
                .status(ChefStatus.ACTIVE)
                .build();

        // Act
        ProfileCreatedEvent event = mapper.toProfileCreatedEvent(chef);

        // Assert
        Assertions.assertEquals(1L, event.getChefId());
        Assertions.assertEquals(10L, event.getAuthUserId());
        Assertions.assertEquals("John", event.getDisplayName());
        Assertions.assertEquals(Station.GRILL, event.getStation());
        Assertions.assertEquals(ChefStatus.ACTIVE, event.getStatus());
    }

    @Test
    void shouldMapUserCreatedEventToChefProfile() {
        // Arrange
        UserCreatedEvent event = new UserCreatedEvent();
        event.setAuthUserId(10L);
        event.setRole("CHEF");
        event.setDisplayName("John");
        event.setStation(Station.GRILL);
        event.setStatus(ChefStatus.ACTIVE);

        // Act
        ChefProfile chef = mapper.toEntity(event);

        // Assert
        Assertions.assertEquals(10L, chef.getAuthUserId());
        Assertions.assertEquals("John", chef.getDisplayName());
        Assertions.assertEquals(Station.GRILL, chef.getStation());
        Assertions.assertEquals(ChefStatus.ACTIVE, chef.getStatus());
    }

    @Test
    void shouldMapUCreatedEventToProfileCreationFailedEvent() {
        // Arrange
        UserCreatedEvent event = new UserCreatedEvent();
        event.setAuthUserId(10L);
        event.setRole("CHEF");
        event.setDisplayName("John");
        event.setStation(Station.GRILL);
        event.setStatus(ChefStatus.ACTIVE);

        // Act
        ProfileCreationFailedEvent failedEvent = mapper.toProfileCreationFailedEvent(event);

        // Assert
        Assertions.assertEquals(10L, failedEvent.getAuthUserId());
        Assertions.assertEquals("CHEF", failedEvent.getRole());
        Assertions.assertEquals("John", failedEvent.getDisplayName());
        Assertions.assertEquals(Station.GRILL, failedEvent.getStation());
        Assertions.assertEquals(ChefStatus.ACTIVE, failedEvent.getStatus());
    }
}

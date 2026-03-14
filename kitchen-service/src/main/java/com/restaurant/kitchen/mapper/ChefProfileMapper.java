package com.restaurant.kitchen.mapper;

import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.events.ProfileCreatedEvent;
import com.restaurant.kitchen.events.ProfileCreationFailedEvent;
import com.restaurant.kitchen.events.UserCreatedEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

//Event to Entity / Entity to Event Mapper
@Mapper(componentModel = "spring")
public interface ChefProfileMapper {

    ChefProfile toEntity (UserCreatedEvent event);

    @Mapping(source = "id" , target = "chefId")
    ProfileCreatedEvent toProfileCreatedEvent(ChefProfile chef);

    ProfileCreationFailedEvent toProfileCreationFailedEvent(UserCreatedEvent event);

}

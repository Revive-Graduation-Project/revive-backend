package com.restaurant.kitchen.mapper;

import com.restaurant.kitchen.dto.ChefProfileDTO;
import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.entity.KitchenTicket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChefProfileMapper {

    ChefProfileDTO toDTO(ChefProfile chefProfile);
    List<ChefProfileDTO> toDTOList(List<ChefProfile> chefs);
}
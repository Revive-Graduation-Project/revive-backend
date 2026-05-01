package com.restaurant.kitchen.mapper;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.entity.KitchenTicket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface KitchenTicketMapper {

    @Mapping(source = "assignedChef.id", target = "assignedChefId")
    KitchenTicketDTO toDTO(KitchenTicket ticket);

    List<KitchenTicketDTO> toDTOList(List<KitchenTicket> tickets);
}

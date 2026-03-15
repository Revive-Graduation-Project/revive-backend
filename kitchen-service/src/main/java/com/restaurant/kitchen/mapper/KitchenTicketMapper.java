package com.restaurant.kitchen.mapper;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.entity.KitchenTicket;
import com.restaurant.kitchen.events.TicketReadyEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface KitchenTicketMapper {
    @Mapping(source = "assignedChef.id" , target = "assignedChefId")
    TicketReadyEvent toTicketReadyEvent (KitchenTicket ticket);

    @Mapping(source = "assignedChef.displayName", target = "chefDisplayName")
    KitchenTicketDTO toDTO(KitchenTicket ticket);

    List<KitchenTicketDTO> toDTOList(List<KitchenTicket> tickets);
}

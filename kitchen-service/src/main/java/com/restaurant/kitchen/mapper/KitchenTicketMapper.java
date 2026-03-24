package com.restaurant.kitchen.mapper;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.entity.KitchenTicket;
import com.restaurant.kitchen.events.OrderCreatedEvent;
import com.restaurant.kitchen.events.ticketEvents.TicketCreatedEvent;
import com.restaurant.kitchen.events.ticketEvents.TicketCreationFailedEvent;
import com.restaurant.kitchen.events.ticketEvents.TicketReadyEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface KitchenTicketMapper {

    @Mapping(source = "assignedChef.id" , target = "assignedChefId")
    @Mapping(source = "id" , target = "ticketId")
    TicketReadyEvent toTicketReadyEvent (KitchenTicket ticket);

    @Mapping(source = "assignedChef.displayName", target = "chefDisplayName")
    KitchenTicketDTO toDTO(KitchenTicket ticket);

    List<KitchenTicketDTO> toDTOList(List<KitchenTicket> tickets);

    TicketCreationFailedEvent toTicketCreationFailedEvent(OrderCreatedEvent event);

    @Mapping(source = "assignedChef.id" , target = "assignedChefId")
    @Mapping(source = "id" , target = "ticketId")
    TicketCreatedEvent toTicketCreatedEvent(KitchenTicket kitchenTicket);
}

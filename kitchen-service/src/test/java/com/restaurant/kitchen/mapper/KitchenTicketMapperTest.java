package com.restaurant.kitchen.mapper;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.entity.KitchenTicket;
import com.restaurant.kitchen.events.ticketEvents.TicketReadyEvent;
import com.restaurant.kitchen.enums.TicketStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KitchenTicketMapperTest {

    private final KitchenTicketMapper mapper = Mappers.getMapper(KitchenTicketMapper.class);

    @Test
    void shouldMapOrderIdCorrectly() {
        KitchenTicket ticket = buildTicket(1L, 100L, 10L);

        TicketReadyEvent event = mapper.toTicketReadyEvent(ticket);

        assertThat(event.getOrderId()).isEqualTo(100L);
    }

    @Test
    void shouldMapAssignedChefIdCorrectly() {
        KitchenTicket ticket = buildTicket(1L, 100L, 10L);

        TicketReadyEvent event = mapper.toTicketReadyEvent(ticket);

        assertThat(event.getAssignedChefId()).isEqualTo(10L);
    }

    @Test
    void shouldReturnNullAssignedChefIdWhenChefIsNull() {
        KitchenTicket ticket = buildTicket(1L, 100L, null);

        TicketReadyEvent event = mapper.toTicketReadyEvent(ticket);

        assertThat(event.getAssignedChefId()).isNull();
    }

    @Test
    void shouldReturnNullWhenTicketIsNull() {
        TicketReadyEvent event = mapper.toTicketReadyEvent(null);

        assertThat(event).isNull();
    }

    @Test
    void shouldReturnEmptyKitchenTicketDTOListWhenKitchenTicketListIsEmpty() {

        List<KitchenTicketDTO> ticketDTOs = mapper.toDTOList(List.of());

        assertThat(ticketDTOs).isEqualTo(List.of());
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private KitchenTicket buildTicket(Long ticketId, Long orderId, Long chefId) {
        ChefProfile chef = null;
        if (chefId != null) {
            chef = new ChefProfile();
            chef.setId(chefId);
        }

        return KitchenTicket.builder()
                .id(ticketId)
                .orderId(orderId)
                .status(TicketStatus.READY)
                .assignedChef(chef)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
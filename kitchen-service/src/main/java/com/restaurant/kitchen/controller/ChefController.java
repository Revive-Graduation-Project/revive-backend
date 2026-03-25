package com.restaurant.kitchen.controller;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.dto.UpdateTicketStatusRequest;
import com.restaurant.kitchen.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kitchen/tickets")
public class ChefController {

    private final TicketService service;

    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<KitchenTicketDTO> updateStatus(@PathVariable Long ticketId,
                                             @Valid @RequestBody UpdateTicketStatusRequest statusRequest) {

        KitchenTicketDTO updatedTicket = service.updateTicketStatus(ticketId, statusRequest.status());
        return ResponseEntity.ok(updatedTicket);
    }

    @GetMapping("/active")
    public ResponseEntity<List<KitchenTicketDTO>> getActiveTickets() {

        List<KitchenTicketDTO> activeTickets = service.getActiveTickets();
        return ResponseEntity.ok(activeTickets);
    }

}

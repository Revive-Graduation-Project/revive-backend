package com.restaurant.kitchen.controller;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.dto.UpdateTicketStatusRequest;
import com.restaurant.kitchen.exception.ForbiddenRoleException;
import com.restaurant.kitchen.service.KitchenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kitchen/tickets")
public class ChefController {

    private final KitchenService service;

    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long ticketId,
                                             @RequestHeader("X-User-Role") String userRole ,
                                             @Valid @RequestBody UpdateTicketStatusRequest statusRequest) {
        if(!"CHEF".equals(userRole))
            throw new ForbiddenRoleException(); //only chef can control tickets

        service.updateTicketStatus(ticketId, statusRequest.status());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/active")
    public ResponseEntity<List<KitchenTicketDTO>> getActiveTickets(@RequestHeader("X-User-Role") String userRole) {

        if(!"CHEF".equals(userRole))
            throw new ForbiddenRoleException();

        List<KitchenTicketDTO> activeTickets = service.getActiveTickets();
        return ResponseEntity.ok(activeTickets);
    }



}

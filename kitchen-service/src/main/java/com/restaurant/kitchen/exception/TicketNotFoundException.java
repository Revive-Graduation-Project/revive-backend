package com.restaurant.kitchen.exception;

import jakarta.annotation.Nullable;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(@Nullable Long id) {
        super(id == null ? "No active tickets found" : "Ticket not found with id: " + id);
    }
}

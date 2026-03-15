package com.restaurant.kitchen.exception;

public class ForbiddenRoleException extends RuntimeException {
    public ForbiddenRoleException() {
        super("Access denied");
    }
}
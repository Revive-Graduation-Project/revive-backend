package com.restaurant.auth.event;

/**
 * Published to restaurant.events.exchange with routing key "user.created"
 * after a new user is successfully persisted.
 */
public class UserCreatedEvent {

    private Long id;
    private String role;

    // Required by Jackson for RabbitMQ deserialization
    public UserCreatedEvent() {
    }

    public UserCreatedEvent(Long id, String role) {
        this.id = id;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

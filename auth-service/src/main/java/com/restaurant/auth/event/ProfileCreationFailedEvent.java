package com.restaurant.auth.event;

/**
 * Consumed from auth.compensation.queue (routing key "chef.profile.failed").
 * Triggers saga compensation: hard-delete the user whose profile could not be
 * created.
 */
public class ProfileCreationFailedEvent {

    private Long id;

    // Required by Jackson for RabbitMQ deserialization
    public ProfileCreationFailedEvent() {
    }

    public ProfileCreationFailedEvent(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}

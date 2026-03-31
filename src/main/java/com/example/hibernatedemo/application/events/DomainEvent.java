package com.example.hibernatedemo.application.events;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class DomainEvent {
    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String eventType;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
    }

    public String getEventId() { return eventId; }
    public LocalDateTime getOccurredOn() { return occurredOn; }
    public String getEventType() { return eventType; }
}

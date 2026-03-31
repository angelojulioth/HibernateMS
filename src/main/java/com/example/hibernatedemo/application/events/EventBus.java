package com.example.hibernatedemo.application.events;

public interface EventBus {
    void publish(DomainEvent event);
}

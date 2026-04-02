package com.example.orderservice.application.events;

public interface EventBus {
    void publish(DomainEvent event);
}

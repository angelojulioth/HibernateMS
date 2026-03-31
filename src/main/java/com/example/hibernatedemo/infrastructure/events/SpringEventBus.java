package com.example.hibernatedemo.infrastructure.events;

import com.example.hibernatedemo.application.events.DomainEvent;
import com.example.hibernatedemo.application.events.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpringEventBus implements EventBus {

    private final ApplicationEventPublisher publisher;

    public SpringEventBus(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DomainEvent event) {
        log.info("Publishing event: {} (id: {})", event.getEventType(), event.getEventId());
        publisher.publishEvent(event);
    }
}

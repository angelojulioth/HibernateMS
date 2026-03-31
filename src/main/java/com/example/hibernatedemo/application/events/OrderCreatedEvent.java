package com.example.hibernatedemo.application.events;

import lombok.Getter;

@Getter
public class OrderCreatedEvent extends DomainEvent {
    private final Long orderId;
    private final String orderNumber;
    private final String customerName;

    public OrderCreatedEvent(Long orderId, String orderNumber, String customerName) {
        super();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.customerName = customerName;
    }
}

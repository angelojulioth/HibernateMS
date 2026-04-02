package com.example.orderservice.application.events;

import com.example.orderservice.domain.enums.OrderStatus;
import lombok.Getter;

@Getter
public class OrderStatusChangedEvent extends DomainEvent {
    private final Long orderId;
    private final OrderStatus oldStatus;
    private final OrderStatus newStatus;

    public OrderStatusChangedEvent(Long orderId, OrderStatus oldStatus, OrderStatus newStatus) {
        super();
        this.orderId = orderId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}

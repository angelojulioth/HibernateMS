package com.example.orderservice.infrastructure.events;

import com.example.orderservice.application.events.OrderCreatedEvent;
import com.example.orderservice.application.events.OrderStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventListener {

    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent - OrderId: {}, OrderNumber: {}, Customer: {}",
                event.getOrderId(), event.getOrderNumber(), event.getCustomerName());
    }

    @EventListener
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Received OrderStatusChangedEvent - OrderId: {}, {} -> {}",
                event.getOrderId(), event.getOldStatus(), event.getNewStatus());
    }
}

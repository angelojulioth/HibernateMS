package com.example.hibernatedemo.application.mediator.handlers;

import com.example.hibernatedemo.application.dto.OrderResponse;
import com.example.hibernatedemo.application.dto.UpdateOrderRequest;
import com.example.hibernatedemo.application.events.EventBus;
import com.example.hibernatedemo.application.events.OrderStatusChangedEvent;
import com.example.hibernatedemo.application.mediator.CommandHandler;
import com.example.hibernatedemo.application.mediator.commands.UpdateOrderCommand;
import com.example.hibernatedemo.domain.entity.Order;
import com.example.hibernatedemo.domain.enums.OrderStatus;
import com.example.hibernatedemo.infrastructure.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateOrderCommandHandler implements CommandHandler<UpdateOrderCommand, OrderResponse> {

    private final OrderRepository orderRepository;
    private final EventBus eventBus;

    @Override
    @Transactional
    public OrderResponse handle(UpdateOrderCommand command) {
        UpdateOrderRequest request = command.getRequest();
        log.info("Handling UpdateOrderCommand for order id: {}", command.getId());

        Order order = orderRepository.findById(command.getId())
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + command.getId()));

        OrderStatus oldStatus = order.getStatus();

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }
        if (request.getShippingAddress() != null) {
            order.setShippingAddress(request.getShippingAddress());
        }
        if (request.getTotalAmount() != null) {
            order.setTotalAmount(request.getTotalAmount());
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order updated with id: {}", updatedOrder.getId());

        if (oldStatus != updatedOrder.getStatus()) {
            eventBus.publish(new OrderStatusChangedEvent(
                    updatedOrder.getId(), oldStatus, updatedOrder.getStatus()));
        }

        return toResponse(updatedOrder);
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}

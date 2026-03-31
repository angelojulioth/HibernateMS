package com.example.hibernatedemo.application.mediator.handlers;

import com.example.hibernatedemo.application.dto.CreateOrderRequest;
import com.example.hibernatedemo.application.dto.OrderResponse;
import com.example.hibernatedemo.application.events.EventBus;
import com.example.hibernatedemo.application.events.OrderCreatedEvent;
import com.example.hibernatedemo.application.mediator.CommandHandler;
import com.example.hibernatedemo.application.mediator.commands.CreateOrderCommand;
import com.example.hibernatedemo.domain.entity.Order;
import com.example.hibernatedemo.infrastructure.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrderCommandHandler implements CommandHandler<CreateOrderCommand, OrderResponse> {

    private final OrderRepository orderRepository;
    private final EventBus eventBus;

    @Override
    @Transactional
    public OrderResponse handle(CreateOrderCommand command) {
        CreateOrderRequest request = command.getRequest();
        log.info("Handling CreateOrderCommand for customer: {}", request.getCustomerName());

        Order order = Order.builder()
                .customerName(request.getCustomerName())
                .orderNumber(request.getOrderNumber())
                .status(request.getStatus())
                .totalAmount(request.getTotalAmount())
                .shippingAddress(request.getShippingAddress())
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with id: {}", savedOrder.getId());

        eventBus.publish(new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                savedOrder.getCustomerName()));

        return toResponse(savedOrder);
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

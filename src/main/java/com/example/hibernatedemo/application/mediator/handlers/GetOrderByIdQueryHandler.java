package com.example.hibernatedemo.application.mediator.handlers;

import com.example.hibernatedemo.application.dto.OrderResponse;
import com.example.hibernatedemo.application.mediator.QueryHandler;
import com.example.hibernatedemo.application.mediator.queries.GetOrderByIdQuery;
import com.example.hibernatedemo.domain.entity.Order;
import com.example.hibernatedemo.infrastructure.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetOrderByIdQueryHandler implements QueryHandler<GetOrderByIdQuery, OrderResponse> {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public OrderResponse handle(GetOrderByIdQuery query) {
        log.debug("Handling GetOrderByIdQuery for id: {}", query.getId());

        Order order = orderRepository.findById(query.getId())
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + query.getId()));

        return toResponse(order);
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

package com.example.hibernatedemo.interfaces.controller;

import com.example.hibernatedemo.application.dto.CreateOrderRequest;
import com.example.hibernatedemo.application.dto.OrderResponse;
import com.example.hibernatedemo.application.dto.UpdateOrderRequest;
import com.example.hibernatedemo.application.mediator.Mediator;
import com.example.hibernatedemo.application.mediator.commands.CreateOrderCommand;
import com.example.hibernatedemo.application.mediator.commands.DeleteOrderCommand;
import com.example.hibernatedemo.application.mediator.commands.UpdateOrderCommand;
import com.example.hibernatedemo.application.mediator.queries.GetOrderByIdQuery;
import com.example.hibernatedemo.application.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final Mediator mediator;
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("POST /api/orders - Creating order for customer: {}", request.getCustomerName());
        OrderResponse response = mediator.send(new CreateOrderCommand(request));
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        log.info("GET /api/orders/{}", id);
        OrderResponse response = mediator.send(new GetOrderByIdQuery(id));
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("GET /api/orders - Fetching page: {}", pageable.getPageNumber());
        Page<OrderResponse> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request) {
        log.info("PUT /api/orders/{} - Updating order", id);
        OrderResponse response = mediator.send(new UpdateOrderCommand(id, request));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        log.info("DELETE /api/orders/{} - Deleting order", id);
        mediator.send(new DeleteOrderCommand(id));
        return ResponseEntity.noContent().build();
    }
}

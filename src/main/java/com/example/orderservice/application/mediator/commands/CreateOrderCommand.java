package com.example.orderservice.application.mediator.commands;

import com.example.orderservice.application.dto.CreateOrderRequest;
import com.example.orderservice.application.dto.OrderResponse;
import com.example.orderservice.application.mediator.Command;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderCommand implements Command<OrderResponse> {
    private CreateOrderRequest request;
}

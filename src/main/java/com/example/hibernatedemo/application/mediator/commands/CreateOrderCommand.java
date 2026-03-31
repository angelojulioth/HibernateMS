package com.example.hibernatedemo.application.mediator.commands;

import com.example.hibernatedemo.application.dto.CreateOrderRequest;
import com.example.hibernatedemo.application.dto.OrderResponse;
import com.example.hibernatedemo.application.mediator.Command;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderCommand implements Command<OrderResponse> {
    private CreateOrderRequest request;
}

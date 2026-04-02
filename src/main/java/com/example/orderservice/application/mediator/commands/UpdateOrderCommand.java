package com.example.orderservice.application.mediator.commands;

import com.example.orderservice.application.dto.OrderResponse;
import com.example.orderservice.application.dto.UpdateOrderRequest;
import com.example.orderservice.application.mediator.Command;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderCommand implements Command<OrderResponse> {
    private Long id;
    private UpdateOrderRequest request;
}

package com.example.hibernatedemo.application.mediator.commands;

import com.example.hibernatedemo.application.dto.OrderResponse;
import com.example.hibernatedemo.application.dto.UpdateOrderRequest;
import com.example.hibernatedemo.application.mediator.Command;
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

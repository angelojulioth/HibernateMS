package com.example.orderservice.application.mediator.queries;

import com.example.orderservice.application.mediator.Query;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetOrderByIdQuery implements Query<com.example.orderservice.application.dto.OrderResponse> {
    private Long id;
}

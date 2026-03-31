package com.example.hibernatedemo.application.mediator.queries;

import com.example.hibernatedemo.application.mediator.Query;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetOrderByIdQuery implements Query<com.example.hibernatedemo.application.dto.OrderResponse> {
    private Long id;
}

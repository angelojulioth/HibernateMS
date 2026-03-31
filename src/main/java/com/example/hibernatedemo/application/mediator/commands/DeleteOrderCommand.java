package com.example.hibernatedemo.application.mediator.commands;

import com.example.hibernatedemo.application.mediator.Command;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteOrderCommand implements Command<Void> {
    private Long id;
}

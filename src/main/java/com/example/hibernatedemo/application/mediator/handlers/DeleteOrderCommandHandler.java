package com.example.hibernatedemo.application.mediator.handlers;

import com.example.hibernatedemo.application.mediator.CommandHandler;
import com.example.hibernatedemo.application.mediator.commands.DeleteOrderCommand;
import com.example.hibernatedemo.infrastructure.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteOrderCommandHandler implements CommandHandler<DeleteOrderCommand, Void> {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public Void handle(DeleteOrderCommand command) {
        log.info("Handling DeleteOrderCommand for order id: {}", command.getId());

        if (!orderRepository.existsById(command.getId())) {
            throw new RuntimeException("Order not found with id: " + command.getId());
        }

        orderRepository.deleteById(command.getId());
        log.info("Order deleted with id: {}", command.getId());
        return null;
    }
}

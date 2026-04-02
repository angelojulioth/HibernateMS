package com.example.orderservice.application.mediator;

public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);
}

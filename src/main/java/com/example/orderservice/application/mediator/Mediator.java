package com.example.orderservice.application.mediator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class Mediator {

    private final Map<Class<?>, CommandHandler<?, ?>> commandHandlers = new HashMap<>();
    private final Map<Class<?>, QueryHandler<?, ?>> queryHandlers = new HashMap<>();

    public Mediator(List<CommandHandler<?, ?>> commandHandlerList,
                    List<QueryHandler<?, ?>> queryHandlerList) {
        for (CommandHandler<?, ?> handler : commandHandlerList) {
            Class<?> commandType = resolveCommandType(handler);
            commandHandlers.put(commandType, handler);
            log.debug("Registered command handler: {} -> {}", commandType.getSimpleName(), handler.getClass().getSimpleName());
        }
        for (QueryHandler<?, ?> handler : queryHandlerList) {
            Class<?> queryType = resolveQueryType(handler);
            queryHandlers.put(queryType, handler);
            log.debug("Registered query handler: {} -> {}", queryType.getSimpleName(), handler.getClass().getSimpleName());
        }
        log.info("Mediator registered {} command handlers and {} query handlers",
                commandHandlers.size(), queryHandlers.size());
    }

    @SuppressWarnings("unchecked")
    public <R> R send(Command<R> command) {
        log.debug("Mediator dispatching command: {}", command.getClass().getSimpleName());
        CommandHandler<?, ?> handler = commandHandlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for command: " + command.getClass().getSimpleName());
        }
        return ((CommandHandler<Command<R>, R>) handler).handle(command);
    }

    @SuppressWarnings("unchecked")
    public <R> R send(Query<R> query) {
        log.debug("Mediator dispatching query: {}", query.getClass().getSimpleName());
        QueryHandler<?, ?> handler = queryHandlers.get(query.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for query: " + query.getClass().getSimpleName());
        }
        return ((QueryHandler<Query<R>, R>) handler).handle(query);
    }

    private Class<?> resolveCommandType(CommandHandler<?, ?> handler) {
        ResolvableType type = ResolvableType.forClass(handler.getClass())
                .as(CommandHandler.class);
        return type.getGeneric(0).resolve();
    }

    private Class<?> resolveQueryType(QueryHandler<?, ?> handler) {
        ResolvableType type = ResolvableType.forClass(handler.getClass())
                .as(QueryHandler.class);
        return type.getGeneric(0).resolve();
    }
}

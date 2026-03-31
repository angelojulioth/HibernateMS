package com.example.hibernatedemo.application.validators;

import com.example.hibernatedemo.infrastructure.repository.OrderRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UniqueOrderNumberValidator implements ConstraintValidator<UniqueOrderNumber, String> {

    private final OrderRepository orderRepository;

    @Override
    public boolean isValid(String orderNumber, ConstraintValidatorContext context) {
        if (orderNumber == null) {
            return true;
        }
        return !orderRepository.existsByOrderNumber(orderNumber);
    }
}

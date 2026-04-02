package com.example.orderservice.application.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueOrderNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueOrderNumber {
    String message() default "Order number already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

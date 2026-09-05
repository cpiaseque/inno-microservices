package com.innowise.orderservice.exception;

import java.io.Serial;

public class ResourceNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 2153815752220987779L;

    private ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException orderNotFound(Long id) {
        return new ResourceNotFoundException("Order not found with id: %d".formatted(id));
    }

    public static ResourceNotFoundException itemNotFound(Integer id) {
        return new ResourceNotFoundException("Item not found with id: %d".formatted(id));
    }
}
package com.innowise.orderservice.exception;

import java.io.Serial;

public class OrderStatusException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 3523983599272262532L;

    private OrderStatusException(String message) {
        super(message);
    }

    public static OrderStatusException orderIsAlreadyCancelled(Long id) {
        return new OrderStatusException("Order with id %d is already cancelled".formatted(id));
    }

    public static OrderStatusException orderIsCompleted(Long id) {
        return new OrderStatusException("Order with id %d is already completed".formatted(id));
    }
}
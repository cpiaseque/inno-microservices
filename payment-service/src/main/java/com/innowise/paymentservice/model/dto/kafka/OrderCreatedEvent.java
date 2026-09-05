package com.innowise.paymentservice.model.dto.kafka;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data transfer object representing an order creation event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    /**
     * Unique identifier of the order.
     */
    @NotNull(message = "Order id is required")
    @Positive(message = "Order id must be positive")
    private Long orderId;

    /**
     * Identifier of the user who placed the order.
     */
    @NotNull(message = "User id is required")
    @Positive(message = "User id must be positive")
    private Long userId;

    /**
     * Total amount to be paid for the order.
     */
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be at least 0.01")
    @Digits(integer = 10, fraction = 2, message = "Payment amount must have max 10 integer and 2 fraction digits")
    private BigDecimal paymentAmount;
}
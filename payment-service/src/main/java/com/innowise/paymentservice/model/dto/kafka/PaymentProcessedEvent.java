package com.innowise.paymentservice.model.dto.kafka;

import com.innowise.paymentservice.model.entity.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

/**
 * Data transfer object representing a payment processing result event.
 */
@Data
@Builder
public class PaymentProcessedEvent {
    /**
     * Unique identifier of the order associated with the payment.
     */
    @NotNull(message = "Order id is required")
    @Positive(message = "Order id must be positive")
    private Long orderId;

    /**
     * Result of the payment processing attempt.
     */
    @NotNull(message = "Payment status is required")
    private PaymentStatus status;
}
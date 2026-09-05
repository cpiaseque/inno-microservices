package com.innowise.orderservice.model.dto.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.innowise.orderservice.model.dto.kafka.enums.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object representing a payment processing result event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
    /**
     * Unique identifier of the order associated with the payment.
     */
    @NotNull(message = "Order id is required")
    @Positive(message = "Order id must be positive")
    private Long orderId;

    /**
     * Result of the payment processing attempt. Serialized as "status" in JSON format.
     */
    @JsonProperty("status")
    @NotBlank(message = "Payment status is required")
    private String paymentStatus;

    public PaymentStatus getPaymentStatus() {
        return PaymentStatus.valueOf(paymentStatus);
    }
}
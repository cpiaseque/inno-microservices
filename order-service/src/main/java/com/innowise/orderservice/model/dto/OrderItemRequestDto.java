package com.innowise.orderservice.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for creating and updating order items.
 */
@Data
@Builder
public class OrderItemRequestDto {
    /**
     * Identifier of the item to be ordered. Must be positive.
     */
    @NotNull(message = "Item id is required")
    @Positive(message = "Item id must be positive")
    private Integer itemId;

    /**
     * Number of units to order for this item. Must be positive.
     */
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
}
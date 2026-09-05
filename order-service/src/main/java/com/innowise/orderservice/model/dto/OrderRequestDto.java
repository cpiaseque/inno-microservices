package com.innowise.orderservice.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object for creating and updating an order.
 * Contains the list of items and their quantities to be ordered.
 * Used as request body in order-related API endpoints.
 *
 * @see OrderItemRequestDto
 */
@Data
@Builder
public class OrderRequestDto {
    /**
     * Collection of items to include in the order. Must contain at least one item.
     */
    @NotNull(message = "Items list is required")
    @Size(min = 1, message = "Order must contain at least one item")
    private List<OrderItemRequestDto> items;
}
package com.innowise.orderservice.model.dto;

import com.innowise.orderservice.model.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object representing a complete order with customer data, item details
 * and calculated total sum of order.
 * Uses as a response body in order-related API endpoints.
 *
 * @see CustomerDto
 * @see OrderItemResponseDto
 */
@Data
@Builder
public class OrderResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 7858535961680780586L;

    /**
     * Customer information retrieved from UserService.
     */
    private CustomerDto customer;

    /**
     * Collection of ordered items.
     */
    private List<OrderItemResponseDto> items;

    /**
     * Calculated total amount for the entire order.
     */
    private BigDecimal totalPrice;

    /**
     * Current status of the order in the workflow.
     */
    private OrderStatus status;
}
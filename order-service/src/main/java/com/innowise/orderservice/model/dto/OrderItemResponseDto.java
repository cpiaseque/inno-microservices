package com.innowise.orderservice.model.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Data Transfer Object representing item details.
 */
@Data
@Builder
public class OrderItemResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 6813888785825133851L;

    /**
     * Display name of the ordered product.
     */
    private String name;

    /**
     * Current price of the product.
     */
    private BigDecimal price;

    /**
     * Number of ordered units.
     */
    private Integer quantity;
}
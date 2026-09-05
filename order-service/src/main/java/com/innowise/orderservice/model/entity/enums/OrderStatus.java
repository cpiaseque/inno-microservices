package com.innowise.orderservice.model.entity.enums;

/**
 * Enumeration represents the possible states of an order in the system workflow.
 * <p>Order lifecycle with payment processing:
 * <pre>
 * {@code
 * PENDING
 *    ↓
 *    ├── COMPLETED       (if payment successful)
 *    └── PAYMENT_FAILED  (if payment rejected)
 * }
 * </pre>
 * </p>
 * <p>
 * CANCELLED can occur at any stage before COMPLETED
 * </p>
 */
public enum OrderStatus {
    PROCESSING, PAYMENT_FAILED, COMPLETED, CANCELLED
}
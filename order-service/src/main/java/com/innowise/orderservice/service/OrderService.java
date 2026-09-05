package com.innowise.orderservice.service;

import com.innowise.orderservice.exception.OrderStatusException;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.model.dto.OrderRequestDto;
import com.innowise.orderservice.model.dto.OrderResponseDto;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.enums.OrderStatus;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

/**
 * Service for managing orders in the system.
 * Provides business logic for order operations and status management including cascade
 * updates to associated orders.
 *
 * @see Order
 * @see OrderRequestDto
 * @see OrderResponseDto
 */
public interface OrderService {
    /**
     * Retrieves an order by its identifier with access control.
     *
     * @param orderId the unique identifier of the order to retrieve
     * @param userId the authenticated user's identifier
     * @return the order details with items and customer information
     * @throws ResourceNotFoundException if the order with given ID does not exist
     * @throws AccessDeniedException if user does not have permission to access the order
     */
    OrderResponseDto getOrderById(Long orderId, Long userId);

    /**
     * Retrieves multiple orders by their identifiers.
     *
     * @param orderIds list of order IDs to retrieve
     * @return list of orders, empty list if no orders found by given IDs
     */
    List<OrderResponseDto> getOrdersByIds(List<Long> orderIds);

    /**
     * Retrieves orders filtered by status values.
     *
     * @param statuses list of order statuses to filter by
     * @return list of orders, empty list if no orders found by given statuses
     */
    List<OrderResponseDto> getOrdersByStatuses(List<OrderStatus> statuses);

    /**
     * Creates a new order for the authenticated user.
     *
     * @param userId the authenticated user's identifier
     * @param orderRequestDto the order creation request
     * @return the created order with items, total sum of the order and customer information
     * @throws ResourceNotFoundException if any specified item does not exist
     */
    OrderResponseDto createOrder(Long userId, OrderRequestDto orderRequestDto);

    /**
     * Updates an existing order's items.
     *
     * @param orderId the unique identifier of the order to update
     * @param orderRequestDto the order update request containing items and their quantities
     * @param userId the authenticated user's identifier
     * @return the updated order with items, total sum of the order and customer information
     * @throws ResourceNotFoundException if order with given ID or any specified item does not exist
     * @throws AccessDeniedException if user doesn't have permission to update the order
     */
    OrderResponseDto updateOrder(Long orderId, OrderRequestDto orderRequestDto, Long userId);

    /**
     * Cancels an order by changing its status. Available only for the order owner.
     *
     * @param orderId the unique identifier of the order to cancel
     * @param userId the authenticated user's identifier (must be order owner)
     * @throws ResourceNotFoundException if the order with given ID does not exist
     * @throws AccessDeniedException if user does not have permission to access the order
     * @throws OrderStatusException if order cannot be cancelled due to its current status
     */
    void cancelOrderAsUser(Long orderId, Long userId);

    /**
     * Permanently deletes an order. Available only for administrators.
     *
     * @param orderId the unique identifier of the order to delete
     * @throws ResourceNotFoundException if the order with given ID does not exist
     * @throws AccessDeniedException if user does not have permission to access the order
     */
    void deleteOrderAsAdmin(Long orderId);
}
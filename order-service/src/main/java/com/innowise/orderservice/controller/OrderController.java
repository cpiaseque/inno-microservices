package com.innowise.orderservice.controller;

import com.innowise.orderservice.exception.OrderStatusException;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.model.dto.OrderRequestDto;
import com.innowise.orderservice.model.dto.OrderResponseDto;
import com.innowise.orderservice.model.entity.enums.OrderStatus;
import com.innowise.orderservice.service.OrderService;
import com.innowise.orderservice.util.Constant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing orders in the system.
 * Provides endpoints for CRUD operations on orders with role-base access control.
 *
 * @see OrderService
 * @see OrderRequestDto
 * @see OrderResponseDto
 */
@Validated
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    /**
     * Retrieves a specific order by its unique identifier.
     * Accessible to USER for own orders and ADMIN with full rights.
     *
     * @param id     the unique identifier of the order to retrieve
     * @param userId the authenticated user's identifier (injected automatically)
     * @return the order details with items and customer information
     * @throws ResourceNotFoundException if the order with given ID does not exist
     * @throws AccessDeniedException     if user does not have permission to access the order
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable("id") Long id,
                                                         @AuthenticationPrincipal Long userId) {
        OrderResponseDto retrievedOrder = orderService.getOrderById(id, userId);

        return ResponseEntity.ok(retrievedOrder);
    }

    /**
     * Retrieves multiple orders by their IDs.
     * Accessible to ADMIN with full rights.
     *
     * @param ids list of order IDs to retrieve
     * @return list of orders, empty list if no orders found by given IDs
     */
    @GetMapping(params = "ids")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByIds(@RequestParam @NotEmpty List<Long> ids) {
        List<OrderResponseDto> retrievedOrders = orderService.getOrdersByIds(ids);

        return ResponseEntity.ok(retrievedOrders);
    }

    /**
     * Retrieves orders filtered by status values.
     * Accessible to ADMIN with full rights.
     *
     * @param statuses list of order statuses to filter by
     * @return list of orders, empty list if no orders found by given statuses
     */
    @GetMapping(params = "statuses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByStatuses(@RequestParam @NotEmpty List<OrderStatus> statuses) {
        List<OrderResponseDto> retrievedOrders = orderService.getOrdersByStatuses(statuses);

        return ResponseEntity.ok(retrievedOrders);
    }

    /**
     * Creates a new order for the authenticated user.
     * Accessible to USER role only.
     *
     * @param requestDto the order creation request containing items and their quantities
     * @param userId     the authenticated user's identifier (injected automatically)
     * @return the created order with items, total sum of the order and customer information
     * @throws ResourceNotFoundException if any specified item does not exist
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponseDto> createOrder(@RequestBody @Valid OrderRequestDto requestDto,
                                                        @AuthenticationPrincipal Long userId) {
        OrderResponseDto createdOrder = orderService.createOrder(userId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    /**
     * Updates an existing order's items.
     * Accessible to USER for own orders only.
     *
     * @param id         the unique identifier of the order to update
     * @param requestDto the order update request containing items and their quantities
     * @param userId     the authenticated user's identifier (injected automatically)
     * @return the updated order with items, total sum of the order and customer information
     * @throws ResourceNotFoundException if order with given ID or any specified item does not exist
     * @throws AccessDeniedException     if user doesn't have permission to update the order
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponseDto> updateOrder(@PathVariable("id") Long id,
                                                        @RequestBody @Valid OrderRequestDto requestDto,
                                                        @AuthenticationPrincipal Long userId) {
        OrderResponseDto updatedOrder = orderService.updateOrder(id, requestDto, userId);

        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * Cancels or deletes an order based on the role.
     * Accessible to USER for own orders and ADMIN with full rights.
     * USER just cancels the order, ADMIN permanently deletes it.
     *
     * @param id             the unique identifier of the order to cancel/delete
     * @param userId         the authenticated user's identifier (injected automatically)
     * @param authentication the authentication object containing user authorities
     * @return empty response
     * @throws ResourceNotFoundException if the order with given ID does not exist
     * @throws AccessDeniedException     if user does not have permission to access the order
     * @throws OrderStatusException      if order cannot be cancelled due to its current status
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable("id") Long id,
                                            @AuthenticationPrincipal Long userId,
                                            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(Constant.ROLE_ADMIN));

        if (isAdmin) {
            orderService.deleteOrderAsAdmin(id);
        } else {
            orderService.cancelOrderAsUser(id, userId);
        }

        return ResponseEntity.noContent().build();
    }
}
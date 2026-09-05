package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.exception.OrderStatusException;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.model.dto.CustomerDto;
import com.innowise.orderservice.model.dto.OrderItemRequestDto;
import com.innowise.orderservice.model.dto.OrderRequestDto;
import com.innowise.orderservice.model.dto.OrderResponseDto;
import com.innowise.orderservice.model.dto.mapper.OrderMapper;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import com.innowise.orderservice.model.entity.enums.OrderStatus;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.service.OrderService;
import com.innowise.orderservice.service.circuitbreaker.UserServiceCircuitBreaker;
import com.innowise.orderservice.service.producer.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.KafkaException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final UserServiceCircuitBreaker circuitBreaker;
    private final OrderEventProducer orderEventProducer;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or @orderRepository.existsOrderByIdAndUserId(#orderId, #userId)")
    public OrderResponseDto getOrderById(Long orderId, Long userId) {
        Order retrievedOrder = orderRepository.findOrderById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.orderNotFound(orderId));

        CustomerDto customer = circuitBreaker.getCustomerInfoOrFallback(retrievedOrder.getUserId());

        return orderMapper.toDto(retrievedOrder, customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersByIds(List<Long> orderIds) {
        List<Order> retrievedOrders = orderRepository.findOrdersByIdIn(orderIds);

        Map<Long, CustomerDto> customers = getIdsToCustomersMap(retrievedOrders);

        return orderMapper.toResponseDtoList(retrievedOrders, customers);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersByStatuses(List<OrderStatus> statuses) {
        List<Order> retrievedOrders = orderRepository.findOrdersByStatusIn(statuses);

        Map<Long, CustomerDto> customers = getIdsToCustomersMap(retrievedOrders);

        return orderMapper.toResponseDtoList(retrievedOrders, customers);
    }

    @Override
    @Transactional
    public OrderResponseDto createOrder(Long userId, OrderRequestDto orderRequestDto) {
        Order newOrder = orderMapper.toEntity(userId, orderRequestDto);

        orderRequestDto.getItems().stream()
                .map(this::convertToOrderItem)
                .forEach(newOrder::addOrderItem);

        Order createdOrder = orderRepository.save(newOrder);

        try {
            orderEventProducer.sendOrderEvent(createdOrder);
        } catch (KafkaException ex) {
            log.error("Failed to send payment request for order with id {}: {}", createdOrder.getId(), ex.getMessage());

            throw ex;
        }

        CustomerDto customer = circuitBreaker.getCustomerInfoOrFallback(userId);

        return orderMapper.toDto(createdOrder, customer);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @orderRepository.existsOrderByIdAndUserId(#orderId, #userId)")
    public OrderResponseDto updateOrder(Long orderId, OrderRequestDto orderRequestDto, Long userId) {
        Order orderToUpdate = orderRepository.findOrderById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.orderNotFound(orderId));

        if (orderToUpdate.getStatus().equals(OrderStatus.COMPLETED)) {
            throw OrderStatusException.orderIsCompleted(orderId);
        }

        orderToUpdate.getOrderItems().clear();
        orderRequestDto.getItems().stream()
                .map(this::convertToOrderItem)
                .forEach(orderToUpdate::addOrderItem);

        Order updatedOrder = orderRepository.save(orderToUpdate);

        try {
            orderEventProducer.sendOrderEvent(updatedOrder);
        } catch (KafkaException ex) {
            log.error("Failed to send payment request for order with id {}: {}", orderId, ex.getMessage());

            throw ex;
        }

        CustomerDto customer = circuitBreaker.getCustomerInfoOrFallback(userId);

        return orderMapper.toDto(updatedOrder, customer);
    }

    @Override
    @Transactional
    @PreAuthorize("@orderRepository.existsOrderByIdAndUserId(#orderId, #userId)")
    public void cancelOrderAsUser(Long orderId, Long userId) {
        Order orderToCancel = orderRepository.findOrderById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.orderNotFound(orderId));

        if (orderToCancel.getStatus().equals(OrderStatus.CANCELLED)) {
            throw OrderStatusException.orderIsAlreadyCancelled(orderId);
        }
        if (orderToCancel.getStatus().equals(OrderStatus.COMPLETED)) {
            throw OrderStatusException.orderIsCompleted(orderId);
        }

        orderRepository.cancelOrderAsUser(orderId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrderAsAdmin(Long orderId) {
        Order orderToDelete = orderRepository.findOrderById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.orderNotFound(orderId));

        orderRepository.delete(orderToDelete);
    }

    private OrderItem convertToOrderItem(OrderItemRequestDto requestDto) {
        return OrderItem.builder()
                .item(itemRepository.findItemById(requestDto.getItemId())
                        .orElseThrow(() -> ResourceNotFoundException.itemNotFound(requestDto.getItemId())))
                .quantity(requestDto.getQuantity())
                .build();
    }

    private Map<Long, CustomerDto> getIdsToCustomersMap(List<Order> retrievedOrders) {
        List<Long> userIds = retrievedOrders.stream()
                .map(Order::getUserId)
                .distinct()
                .toList();

        return userIds.isEmpty()
                ? Collections.emptyMap()
                : circuitBreaker.getCustomersInfoOrFallbackMap(userIds);
    }
}
package com.innowise.orderservice.model.dto.mapper;

import com.innowise.orderservice.model.dto.CustomerDto;
import com.innowise.orderservice.model.dto.OrderRequestDto;
import com.innowise.orderservice.model.dto.OrderResponseDto;
import com.innowise.orderservice.model.dto.kafka.OrderCreatedEvent;
import com.innowise.orderservice.model.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        uses = OrderItemMapper.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {
    Order toEntity(Long userId, OrderRequestDto requestDto);

    @Mapping(target = "items", source = "order.orderItems")
    @Mapping(target = "totalPrice", expression = "java(calculateTotalPrice(order))")
    OrderResponseDto toDto(Order order, CustomerDto customer);

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "paymentAmount", expression = "java(calculateTotalPrice(order))")
    OrderCreatedEvent toEvent(Order order);

    default List<OrderResponseDto> toResponseDtoList(List<Order> orders, Map<Long, CustomerDto> customerMap) {
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        return orders.stream()
                .map(order -> toDto(order, customerMap.get(order.getUserId())))
                .collect(Collectors.toList());
    }

    default BigDecimal calculateTotalPrice(Order order) {
        if (order.getOrderItems() == null) {
            return BigDecimal.ZERO;
        }
        return order.getOrderItems().stream()
                .map(orderItem -> orderItem.getItem().getPrice()
                        .multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
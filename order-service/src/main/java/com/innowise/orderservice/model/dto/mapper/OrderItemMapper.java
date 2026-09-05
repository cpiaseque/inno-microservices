package com.innowise.orderservice.model.dto.mapper;

import com.innowise.orderservice.model.dto.OrderItemResponseDto;
import com.innowise.orderservice.model.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
    @Mapping(source = "item.name", target = "name")
    @Mapping(source = "item.price", target = "price")
    OrderItemResponseDto toDto(OrderItem orderItem);
}
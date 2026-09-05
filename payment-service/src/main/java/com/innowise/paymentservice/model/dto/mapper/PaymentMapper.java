package com.innowise.paymentservice.model.dto.mapper;

import com.innowise.paymentservice.model.dto.kafka.OrderCreatedEvent;
import com.innowise.paymentservice.model.dto.kafka.PaymentProcessedEvent;
import com.innowise.paymentservice.model.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {
    Payment toEntity(OrderCreatedEvent event);

    PaymentProcessedEvent toEvent(Payment payment);
}
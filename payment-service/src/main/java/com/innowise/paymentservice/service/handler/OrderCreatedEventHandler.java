package com.innowise.paymentservice.service.handler;

import com.innowise.paymentservice.model.dto.kafka.OrderCreatedEvent;
import com.innowise.paymentservice.model.dto.kafka.PaymentProcessedEvent;
import com.innowise.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer component which handles order creation events and publishes the result back.
 *
 * @see OrderCreatedEvent
 * @see PaymentProcessedEvent
 */
@Component
@KafkaListener(topics = "${orders.events.topic}")
@RequiredArgsConstructor
public class OrderCreatedEventHandler {
    private final PaymentService paymentService;

    /**
     * Processes order creation events and initiates payment processing workflow.
     *
     * @param orderCreatedEvent the event received from Kafka
     */
    @KafkaHandler
    public void handleOrderCreatedEvent(OrderCreatedEvent orderCreatedEvent) {
        paymentService.createPayment(orderCreatedEvent);
    }
}
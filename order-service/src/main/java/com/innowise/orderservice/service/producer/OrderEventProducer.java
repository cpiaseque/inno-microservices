package com.innowise.orderservice.service.producer;

import com.innowise.orderservice.model.dto.kafka.OrderCreatedEvent;
import com.innowise.orderservice.model.dto.mapper.OrderMapper;
import com.innowise.orderservice.model.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

/**
 * Producer for sending events to Kafka. Responsible for asynchronously publishing order creation events
 * to the configured Kafka topic.
 *
 * @see OrderCreatedEvent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {
    private final OrderMapper orderMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${orders.events.topic}")
    private String ordersEventsTopic;

    /**
     * Asynchronously sends an order creation event to Kafka.
     * Blocks until confirmation or timeout/error, throwing an exception if failed.
     *
     * @param createdOrder the Order entity that was successfully persisted and should be published as an event
     */
    public void sendOrderEvent(Order createdOrder) {
        OrderCreatedEvent event = orderMapper.toEvent(createdOrder);

        try {
            kafkaTemplate.send(ordersEventsTopic, event).get();

            log.info("Payment request for order with id {} has been sent successfully", createdOrder.getId());
        } catch (ExecutionException | InterruptedException ex) {
            throw new KafkaException("Payment service is temporarily unavailable.", ex);
        }
    }
}
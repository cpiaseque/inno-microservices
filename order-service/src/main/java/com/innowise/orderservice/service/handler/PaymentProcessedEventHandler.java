package com.innowise.orderservice.service.handler;

import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.model.dto.kafka.PaymentProcessedEvent;
import com.innowise.orderservice.model.dto.kafka.enums.PaymentStatus;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.enums.OrderStatus;
import com.innowise.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer component which listens to the payment events topic and processes messages
 * to update order statuses based on payment outcomes.
 *
 * @see PaymentProcessedEvent
 */
@Component
@KafkaListener(topics = "${payments.events.topic}")
@RequiredArgsConstructor
public class PaymentProcessedEventHandler {
    private final OrderRepository orderRepository;

    /**
     * Processes payment result events and updates corresponding order status.
     *
     * @param paymentProcessedEvent the event received from Kafka
     */
    @KafkaHandler
    public void handlePaymentProcessedEvent(PaymentProcessedEvent paymentProcessedEvent) {
        Order order = orderRepository.findById(paymentProcessedEvent.getOrderId())
                .orElseThrow(() -> ResourceNotFoundException.orderNotFound(paymentProcessedEvent.getOrderId()));

        if (paymentProcessedEvent.getPaymentStatus().equals(PaymentStatus.SUCCESS)) {
            order.setStatus(OrderStatus.COMPLETED);
        } else if (paymentProcessedEvent.getPaymentStatus().equals(PaymentStatus.FAILED)) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
        }

        orderRepository.save(order);
    }
}
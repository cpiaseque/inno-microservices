package com.innowise.paymentservice.service.producer;

import com.innowise.paymentservice.model.dto.kafka.PaymentProcessedEvent;
import com.innowise.paymentservice.model.dto.mapper.PaymentMapper;
import com.innowise.paymentservice.model.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

/**
 * Producer for sending events to Kafka. Responsible for asynchronously publishing payment processing events
 * to the configured Kafka topic.
 *
 * @see PaymentProcessedEvent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {
    private final PaymentMapper paymentMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${payments.events.topic}")
    private String paymentsEventsTopic;

    /**
     * Asynchronously sends a payment processing event to Kafka.
     * Blocks until confirmation or timeout/error, throwing an exception if failed.
     *
     * @param createdPayment the Payment entity that was successfully persisted and should be published as an event
     */
    public void sendPaymentProcessedEvent(Payment createdPayment) {
        PaymentProcessedEvent event = paymentMapper.toEvent(createdPayment);

        try {
            kafkaTemplate.send(paymentsEventsTopic, event).get();

            log.info("Payment response for order id {} has been sent successfully with id: {}",
                    createdPayment.getOrderId(), createdPayment.getId());
        } catch (ExecutionException | RuntimeException ex) {
            throw new KafkaException("Fatal error during Kafka send operation.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new KafkaException("Thread interrupted during Kafka send operation.", ex);
        }
    }
}
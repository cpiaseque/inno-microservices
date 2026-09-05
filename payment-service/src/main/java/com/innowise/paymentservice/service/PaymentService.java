package com.innowise.paymentservice.service;

import com.innowise.paymentservice.client.RandomNumberApiClient;
import com.innowise.paymentservice.model.dto.kafka.OrderCreatedEvent;
import com.innowise.paymentservice.model.entity.Payment;

/**
 * Service for payment processing operations.
 * Handles business logic of payment creation and processing.
 *
 * @see RandomNumberApiClient
 * @see OrderCreatedEvent
 * @see Payment
 */
public interface PaymentService {
    /**
     * Creates and processes a payment based on an order creation event and external API-service.
     *
     * @param event the order creation event containing payment details
     */
    void createPayment(OrderCreatedEvent event);
}
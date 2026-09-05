package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.RandomNumberApiClient;
import com.innowise.paymentservice.model.dto.kafka.OrderCreatedEvent;
import com.innowise.paymentservice.model.dto.mapper.PaymentMapper;
import com.innowise.paymentservice.model.entity.Payment;
import com.innowise.paymentservice.model.entity.enums.PaymentStatus;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.PaymentService;
import com.innowise.paymentservice.service.producer.PaymentEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RandomNumberApiClient randomNumberApiClient;
    private final PaymentEventProducer paymentEventProducer;

    @Override
    @Transactional
    public void createPayment(OrderCreatedEvent event) {
        if (paymentRepository.existsByOrderIdAndStatus(event.getOrderId(), PaymentStatus.SUCCESS)) {
            log.info("Payment for order {} already exists , skipping duplicate event.", event.getOrderId());
            return;
        }

        Payment newPayment = paymentMapper.toEntity(event);

        PaymentStatus paymentOperationResult = randomNumberApiClient.determinePaymentStatus();
        newPayment.setStatus(paymentOperationResult);

        Payment createdPayment = paymentRepository.save(newPayment);

        paymentEventProducer.sendPaymentProcessedEvent(createdPayment);
    }
}
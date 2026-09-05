package com.innowise.paymentservice.service;

import com.innowise.paymentservice.client.RandomNumberApiClient;
import com.innowise.paymentservice.model.dto.kafka.OrderCreatedEvent;
import com.innowise.paymentservice.model.dto.mapper.PaymentMapperImpl;
import com.innowise.paymentservice.model.entity.Payment;
import com.innowise.paymentservice.model.entity.enums.PaymentStatus;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.impl.PaymentServiceImpl;
import com.innowise.paymentservice.service.producer.PaymentEventProducer;
import com.innowise.paymentservice.util.TestConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.KafkaException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PaymentServiceImpl.class,
        PaymentMapperImpl.class
})
class PaymentServiceTest {
    @MockitoBean
    private RandomNumberApiClient randomNumberApiClient;

    @MockitoBean
    private PaymentRepository paymentRepository;

    @MockitoBean
    private PaymentEventProducer paymentEventProducer;

    @Autowired
    private PaymentService paymentService;

    private static OrderCreatedEvent testOrderCreatedEvent;
    private static Payment testPayment;

    @BeforeAll
    static void beforeAll() {
        testOrderCreatedEvent = OrderCreatedEvent.builder()
                .orderId(TestConstants.ID)
                .userId(TestConstants.ID)
                .paymentAmount(BigDecimal.TEN)
                .build();
        testPayment = Payment.builder().build();
    }

    @Test
    void createPaymentWithSuccessStatusTest() {
        when(paymentRepository.existsByOrderIdAndStatus(TestConstants.ID, PaymentStatus.SUCCESS)).thenReturn(false);
        when(randomNumberApiClient.determinePaymentStatus()).thenReturn(PaymentStatus.SUCCESS);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        paymentService.createPayment(testOrderCreatedEvent);

        verify(paymentRepository, times(1)).existsByOrderIdAndStatus(TestConstants.ID, PaymentStatus.SUCCESS);
        verify(randomNumberApiClient, times(1)).determinePaymentStatus();
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentEventProducer, times(1)).sendPaymentProcessedEvent(any(Payment.class));
    }

    @Test
    void createPaymentWithFailedStatusTest() {
        when(paymentRepository.existsByOrderIdAndStatus(TestConstants.ID, PaymentStatus.SUCCESS)).thenReturn(false);
        when(randomNumberApiClient.determinePaymentStatus()).thenReturn(PaymentStatus.FAILED);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        paymentService.createPayment(testOrderCreatedEvent);

        verify(paymentRepository, times(1)).existsByOrderIdAndStatus(TestConstants.ID, PaymentStatus.SUCCESS);
        verify(randomNumberApiClient, times(1)).determinePaymentStatus();
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentEventProducer, times(1)).sendPaymentProcessedEvent(any(Payment.class));
    }

    @Test
    void createPaymentWhenExistsByOrderIdTest() {
        when(paymentRepository.existsByOrderIdAndStatus(TestConstants.ID, PaymentStatus.SUCCESS)).thenReturn(true);

        paymentService.createPayment(testOrderCreatedEvent);

        verify(paymentRepository, times(1)).existsByOrderIdAndStatus(TestConstants.ID, PaymentStatus.SUCCESS);
    }

    @Test
    void createPaymentWhenKafkaThrewExceptionTest() {
        when(paymentRepository.existsByOrderIdAndStatus(TestConstants.ID, PaymentStatus.SUCCESS)).thenReturn(false);
        when(randomNumberApiClient.determinePaymentStatus()).thenReturn(PaymentStatus.FAILED);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        doThrow(new KafkaException("Kafka is unavailable")).when(paymentEventProducer).sendPaymentProcessedEvent(testPayment);

        assertThatThrownBy(() -> paymentService.createPayment(testOrderCreatedEvent))
                .isInstanceOf(KafkaException.class)
                .hasMessageContaining("Kafka is unavailable");

        verify(paymentRepository, times(1)).existsByOrderIdAndStatus(TestConstants.ID, PaymentStatus.SUCCESS);
        verify(randomNumberApiClient, times(1)).determinePaymentStatus();
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentEventProducer, times(1)).sendPaymentProcessedEvent(any(Payment.class));
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(randomNumberApiClient, paymentRepository, paymentEventProducer);
    }
}
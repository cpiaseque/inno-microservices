package com.innowise.orderservice.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.innowise.orderservice.model.dto.kafka.PaymentProcessedEvent;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.enums.OrderStatus;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.util.TestConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EmbeddedKafka(topics = "${payments.events.topic}")
@SpringBootTest
class OrderKafkaIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @DynamicPropertySource
    private static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.cloud.openfeign.client.config.user-service.url",
                () -> "http://localhost:%s/api/v1".formatted(userServiceMock.getPort()));
        registry.add("spring.kafka.bootstrap-servers", () -> "${spring.embedded.kafka.brokers}");
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
        registry.add("spring.kafka.consumer.properties.spring.json.use.type.headers", () -> "false");
        registry.add("spring.kafka.consumer.properties.spring.json.value.default.type",
                () -> "com.innowise.orderservice.model.dto.kafka.PaymentProcessedEvent");
    }

    @RegisterExtension
    private static final WireMockExtension userServiceMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${payments.events.topic}")
    private String paymentsEventsTopic;

    private Order orderInDb;

    @BeforeEach
    void setUp() {
        orderInDb = Order.builder()
                .status(OrderStatus.PROCESSING)
                .build();

        orderRepository.save(orderInDb);
    }

    @Test
    void paymentSuccessfulKafkaEventReceivedIntegrationTest() {
        PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .orderId(orderInDb.getId())
                .paymentStatus(TestConstant.PAYMENT_STATUS_SUCCESS)
                .build();

        kafkaTemplate.send(paymentsEventsTopic, event);

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Order updatedOrder = orderRepository.findById(orderInDb.getId()).orElseThrow();
                    assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                });
    }

    @Test
    void paymentFailedKafkaEventReceivedIntegrationTest() {
        PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .orderId(orderInDb.getId())
                .paymentStatus(TestConstant.PAYMENT_STATUS_FAILED)
                .build();

        kafkaTemplate.send(paymentsEventsTopic, event);

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Order updatedOrder = orderRepository.findById(orderInDb.getId()).orElseThrow();
                    assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
                });
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
    }
}
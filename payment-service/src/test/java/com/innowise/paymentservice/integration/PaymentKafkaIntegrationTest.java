package com.innowise.paymentservice.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.innowise.paymentservice.model.dto.kafka.OrderCreatedEvent;
import com.innowise.paymentservice.model.entity.Payment;
import com.innowise.paymentservice.model.entity.enums.PaymentStatus;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.util.TestConstants;
import org.junit.jupiter.api.AfterEach;
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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(topics = "${orders.events.topic}")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
class PaymentKafkaIntegrationTest {
    @Container
    private static final MongoDBContainer mongoDB = new MongoDBContainer("mongo:latest");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDB::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", () -> "${spring.embedded.kafka.brokers}");
        registry.add("external-api.url", externalApiMock::baseUrl);
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
        registry.add("spring.kafka.consumer.properties.spring.json.use.type.headers", () -> "false");
        registry.add("spring.kafka.consumer.properties.spring.json.value.default.type",
                () -> "com.innowise.paymentservice.model.dto.kafka.OrderCreatedEvent");
    }

    @RegisterExtension
    private static final WireMockExtension externalApiMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String EXTERNAL_API_URL = "/";

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${orders.events.topic}")
    private String ordersEventsTopic;

    @Test
    void orderCreatedKafkaFailedEventReceivedIntegrationTest() {
        OrderCreatedEvent orderEvent = OrderCreatedEvent.builder()
                .orderId(TestConstants.ID)
                .userId(TestConstants.ID)
                .paymentAmount(BigDecimal.TEN)
                .build();

        externalApiMock.stubFor(WireMock.get(urlPathEqualTo(EXTERNAL_API_URL))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[1]")));

        kafkaTemplate.send(ordersEventsTopic, orderEvent);

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Payment payment = paymentRepository.findPaymentByOrderId(TestConstants.ID).orElse(null);
                    assertThat(payment).isNotNull();
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
                });
    }

    @Test
    void orderCreatedKafkaSuccessEventReceivedIntegrationTest() {
        OrderCreatedEvent orderEvent = OrderCreatedEvent.builder()
                .orderId(TestConstants.ID)
                .userId(TestConstants.ID)
                .paymentAmount(BigDecimal.TEN)
                .build();

        externalApiMock.stubFor(WireMock.get(urlPathEqualTo(EXTERNAL_API_URL))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[2]")));

        kafkaTemplate.send(ordersEventsTopic, orderEvent);

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Payment payment = paymentRepository.findPaymentByOrderId(TestConstants.ID).orElse(null);
                    assertThat(payment).isNotNull();
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
                });
    }

    @Test
    void orderCreatedKafkaExternalApiIsUnavailableIntegrationTest() {
        OrderCreatedEvent orderEvent = OrderCreatedEvent.builder()
                .orderId(TestConstants.ID)
                .userId(TestConstants.ID)
                .paymentAmount(BigDecimal.TEN)
                .build();

        externalApiMock.stubFor(WireMock.get(urlPathEqualTo(EXTERNAL_API_URL))
                .willReturn(serverError()));

        kafkaTemplate.send(ordersEventsTopic, orderEvent);

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Payment payment = paymentRepository.findPaymentByOrderId(TestConstants.ID).orElse(null);
                    assertThat(payment).isNotNull();
                    assertThat(payment.getStatus()).isIn(PaymentStatus.SUCCESS, PaymentStatus.FAILED);
                });
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
    }
}
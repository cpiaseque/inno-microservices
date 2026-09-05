package com.innowise.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.innowise.orderservice.model.dto.CustomerDto;
import com.innowise.orderservice.model.dto.OrderItemRequestDto;
import com.innowise.orderservice.model.dto.OrderRequestDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import com.innowise.orderservice.model.entity.enums.OrderStatus;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.util.TestConstant;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EmbeddedKafka(topics = "${payments.events.topic}")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderControllerTest {
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
    }

    @RegisterExtension
    private static final WireMockExtension userServiceMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String INTERNAL_URL = "/orders";
    private static final String EXTERNAL_SERVICE_URL = "/api/v1/users";

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static CustomerDto testCustomer;

    @BeforeAll
    static void beforeAll() {
        testCustomer = CustomerDto.builder()
                .userId(TestConstant.LONG_ID)
                .name(TestConstant.USER_NAME)
                .surname(TestConstant.USER_NAME)
                .email(TestConstant.USER_EMAIL)
                .build();
    }

    @Test
    void getOrderByIdWhenUserIsOwnerIntegrationTest() throws Exception {
        Order orderInDb = saveFullOrderWithItem();

        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_USER_WITH_PREFIX);

        userServiceMock.stubFor(WireMock.get(
                        urlEqualTo(String.format("%s/%d", EXTERNAL_SERVICE_URL, TestConstant.LONG_ID)))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(testCustomer))));

        mockMvc.perform(get(String.format("%s/%d", INTERNAL_URL, orderInDb.getId())))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER).exists(),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_NAME).value(TestConstant.USER_NAME),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_SURNAME).value(TestConstant.USER_NAME),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_EMAIL).value(TestConstant.USER_EMAIL),
                        jsonPath(TestConstant.JSON_PATH_ITEMS).isArray(),
                        jsonPath(TestConstant.JSON_PATH_ITEMS, hasSize(1)),
                        jsonPath(TestConstant.JSON_PATH_ITEM_NAME).value(TestConstant.ITEM_NAME),
                        jsonPath(TestConstant.JSON_PATH_ITEM_PRICE).value(TestConstant.ITEM_PRICE),
                        jsonPath(TestConstant.JSON_PATH_ITEM_QUANTITY).value(TestConstant.ITEM_QUANTITY),
                        jsonPath(TestConstant.JSON_PATH_TOTAL_PRICE)
                                .value(TestConstant.ITEM_PRICE.multiply(BigDecimal.valueOf(TestConstant.ITEM_QUANTITY))),
                        jsonPath(TestConstant.JSON_PATH_STATUS).value(OrderStatus.PROCESSING.name())
                );
    }

    @Test
    void getOrderByIdWhenUserIsNotRegisteredIntegrationTest() throws Exception {
        Order orderInDb = orderRepository.save(DtoBuilder.buildOrder());

        mockMvc.perform(get(String.format("%s/%d", INTERNAL_URL, orderInDb.getId())))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.UNAUTHORIZED.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value("Invalid authentication token"),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void getOrderByIdWhenUserIsNotOwnerIntegrationTest() throws Exception {
        Order orderInDb = orderRepository.save(DtoBuilder.buildOrder());

        setAuthentication(2L, TestConstant.ROLE_USER_WITH_PREFIX);

        mockMvc.perform(get(String.format("%s/%d", INTERNAL_URL, orderInDb.getId())))
                .andExpectAll(
                        status().isForbidden(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.FORBIDDEN.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value("Access Denied"),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void getOrderByIdWhenUserIsAdminIntegrationTest() throws Exception {
        Order orderInDb = saveFullOrderWithItem();

        setAuthentication(2L, TestConstant.ROLE_ADMIN_WITH_PREFIX);

        userServiceMock.stubFor(WireMock.get(
                        urlEqualTo(String.format("%s/%d", EXTERNAL_SERVICE_URL, orderInDb.getId())))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(testCustomer))));

        mockMvc.perform(get(String.format("%s/%d", INTERNAL_URL, orderInDb.getId())))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER).exists(),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_NAME).value(TestConstant.USER_NAME),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_SURNAME).value(TestConstant.USER_NAME),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_EMAIL).value(TestConstant.USER_EMAIL),
                        jsonPath(TestConstant.JSON_PATH_ITEMS).isArray(),
                        jsonPath(TestConstant.JSON_PATH_ITEMS, hasSize(1)),
                        jsonPath(TestConstant.JSON_PATH_ITEM_NAME).value(TestConstant.ITEM_NAME),
                        jsonPath(TestConstant.JSON_PATH_ITEM_PRICE).value(TestConstant.ITEM_PRICE),
                        jsonPath(TestConstant.JSON_PATH_ITEM_QUANTITY).value(TestConstant.ITEM_QUANTITY),
                        jsonPath(TestConstant.JSON_PATH_TOTAL_PRICE)
                                .value(TestConstant.ITEM_PRICE.multiply(BigDecimal.valueOf(TestConstant.ITEM_QUANTITY))),
                        jsonPath(TestConstant.JSON_PATH_STATUS).value(OrderStatus.PROCESSING.name())
                );
    }

    @Test
    void getOrderByIdWhenUserServiceIsUnavailableIntegrationTest() throws Exception {
        Order orderInDb = saveFullOrderWithItem();

        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_ADMIN_WITH_PREFIX);

        userServiceMock.stubFor(WireMock.get(
                        urlEqualTo(String.format("%s/%d", EXTERNAL_SERVICE_URL, orderInDb.getId())))
                .willReturn(serverError()));

        mockMvc.perform(get(String.format("%s/%d", INTERNAL_URL, orderInDb.getId())))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER).exists(),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_NAME).doesNotExist(),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_SURNAME).doesNotExist(),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_EMAIL).doesNotExist(),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_ERROR_MESSAGE)
                                .value("User information temporary unavailable"),
                        jsonPath(TestConstant.JSON_PATH_ITEMS).isArray(),
                        jsonPath(TestConstant.JSON_PATH_ITEMS, hasSize(1)),
                        jsonPath(TestConstant.JSON_PATH_ITEM_NAME).value(TestConstant.ITEM_NAME),
                        jsonPath(TestConstant.JSON_PATH_ITEM_PRICE).value(TestConstant.ITEM_PRICE),
                        jsonPath(TestConstant.JSON_PATH_ITEM_QUANTITY).value(TestConstant.ITEM_QUANTITY),
                        jsonPath(TestConstant.JSON_PATH_TOTAL_PRICE)
                                .value(TestConstant.ITEM_PRICE.multiply(BigDecimal.valueOf(TestConstant.ITEM_QUANTITY))),
                        jsonPath(TestConstant.JSON_PATH_STATUS).value(OrderStatus.PROCESSING.name())
                );
    }

    @Test
    void getOrderByIdWhenOrderDoesNotExistIntegrationTest() throws Exception {
        setAuthentication(2L, TestConstant.ROLE_ADMIN_WITH_PREFIX);

        mockMvc.perform(get(String.format("%s/%d", INTERNAL_URL, TestConstant.LONG_ID)))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.NOT_FOUND.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString("Order not found with id")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString(String.valueOf(TestConstant.LONG_ID))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void getOrdersByIdsSuccessfulIntegrationTest() throws Exception {
        Order orderInDb = saveFullOrderWithItem();
        String ids = String.format("%d,%d", orderInDb.getId(), orderInDb.getId() + 1);

        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_ADMIN_WITH_PREFIX);

        userServiceMock.stubFor(WireMock.get(urlPathEqualTo(EXTERNAL_SERVICE_URL))
                .withQueryParam("ids", matching(String.valueOf(TestConstant.LONG_ID)))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(List.of(testCustomer)))));

        mockMvc.perform(get(INTERNAL_URL)
                        .param("ids", ids))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY).isArray(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY, hasSize(1))
                );
    }

    @Test
    void getOrdersByIdsWhenUserServiceIsUnavailableIntegrationTest() throws Exception {
        Order orderInDb = saveFullOrderWithItem();
        String ids = String.format("%d,%d", orderInDb.getId(), orderInDb.getId() + 1);

        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_ADMIN_WITH_PREFIX);

        userServiceMock.stubFor(WireMock.get(urlPathEqualTo(EXTERNAL_SERVICE_URL))
                .withQueryParam("ids", matching(String.valueOf(TestConstant.LONG_ID)))
                .willReturn(serverError()));

        mockMvc.perform(get(INTERNAL_URL)
                        .param("ids", ids))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY).isArray(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY, hasSize(1))
                );
    }

    @Test
    void getOrdersByIdsWhenOrderDoesNotExistIntegrationTest() throws Exception {
        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_ADMIN_WITH_PREFIX);

        mockMvc.perform(get(INTERNAL_URL)
                        .param("ids", String.valueOf(TestConstant.LONG_ID)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY).isArray(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY).isEmpty()
                );
    }

    @Test
    void getOrdersByIdsWhenUserDoesNotHaveRightsIntegrationTest() throws Exception {
        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_USER_WITH_PREFIX);

        mockMvc.perform(get(INTERNAL_URL)
                        .param("ids", String.valueOf(TestConstant.LONG_ID)))
                .andExpectAll(
                        status().isForbidden(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.FORBIDDEN.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value("Access Denied"),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void getOrdersByStatusesSuccessfulIntegrationTest() throws Exception {
        Order orderInDb = saveFullOrderWithItem();

        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_ADMIN_WITH_PREFIX);

        userServiceMock.stubFor(WireMock.get(urlPathEqualTo(EXTERNAL_SERVICE_URL))
                .withQueryParam("ids", matching(String.valueOf(TestConstant.LONG_ID)))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(List.of(testCustomer)))));

        mockMvc.perform(get(INTERNAL_URL)
                        .param("statuses", String.valueOf(orderInDb.getStatus())))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY).isArray(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY, hasSize(1))
                );
    }

    @Test
    void getOrdersByStatusesWhenUserServiceIsUnavailableIntegrationTest() throws Exception {
        Order orderInDb = saveFullOrderWithItem();

        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_ADMIN_WITH_PREFIX);

        userServiceMock.stubFor(WireMock.get(urlPathEqualTo(EXTERNAL_SERVICE_URL))
                .withQueryParam("ids", matching(String.valueOf(TestConstant.LONG_ID)))
                .willReturn(serverError()));

        mockMvc.perform(get(INTERNAL_URL)
                        .param("statuses", String.valueOf(orderInDb.getStatus())))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY).isArray(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY, hasSize(1))
                );
    }

    @Test
    void getOrdersByStatusesWhenOrderDoesNotExistIntegrationTest() throws Exception {
        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_ADMIN_WITH_PREFIX);

        mockMvc.perform(get(INTERNAL_URL)
                        .param("statuses", String.valueOf(OrderStatus.CANCELLED)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY).isArray(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY).isEmpty()
                );
    }

    @Test
    void getOrdersByStatusesWhenUserDoesNotHaveRightsIntegrationTest() throws Exception {
        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_USER_WITH_PREFIX);

        mockMvc.perform(get(INTERNAL_URL)
                        .param("statuses", String.valueOf(OrderStatus.PROCESSING)))
                .andExpectAll(
                        status().isForbidden(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.FORBIDDEN.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value("Access Denied"),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void getOrdersByIdsWhenParameterIsMissingIntegrationTest() throws Exception {
        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_ADMIN_WITH_PREFIX);

        mockMvc.perform(get(INTERNAL_URL))
                .andExpectAll(
                        status().isInternalServerError(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS)
                                .value(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString("Parameter conditions")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString("not met for actual request parameters")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void getOrdersByIdsWhenParameterIsIncorrectIntegrationTest() throws Exception {
        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_ADMIN_WITH_PREFIX);

        mockMvc.perform(get(INTERNAL_URL)
                        .param("ids", ""))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.BAD_REQUEST.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value("Constraint violation"),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_DETAILS, hasItem(containsString("ids"))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_DETAILS, hasItem(containsString("must not be empty"))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void createOrderSuccessfulIntegrationTest() throws Exception {
        Item itemInDb = itemRepository.save(DtoBuilder.buildItem());
        OrderItemRequestDto item = OrderItemRequestDto.builder()
                .itemId(itemInDb.getId())
                .quantity(TestConstant.ITEM_QUANTITY)
                .build();
        OrderRequestDto requestDto = OrderRequestDto.builder()
                .items(List.of(item))
                .build();

        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_USER_WITH_PREFIX);

        userServiceMock.stubFor(WireMock.get(
                        urlEqualTo(String.format("%s/%d", EXTERNAL_SERVICE_URL, TestConstant.LONG_ID)))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(testCustomer))));

        mockMvc.perform(post(INTERNAL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER).exists(),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_NAME).value(TestConstant.USER_NAME),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_SURNAME).value(TestConstant.USER_NAME),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_EMAIL).value(TestConstant.USER_EMAIL),
                        jsonPath(TestConstant.JSON_PATH_ITEMS).isArray(),
                        jsonPath(TestConstant.JSON_PATH_ITEMS, hasSize(1)),
                        jsonPath(TestConstant.JSON_PATH_ITEM_NAME).value(TestConstant.ITEM_NAME),
                        jsonPath(TestConstant.JSON_PATH_ITEM_PRICE).value(TestConstant.ITEM_PRICE),
                        jsonPath(TestConstant.JSON_PATH_ITEM_QUANTITY).value(TestConstant.ITEM_QUANTITY),
                        jsonPath(TestConstant.JSON_PATH_TOTAL_PRICE)
                                .value(TestConstant.ITEM_PRICE.multiply(BigDecimal.valueOf(TestConstant.ITEM_QUANTITY))),
                        jsonPath(TestConstant.JSON_PATH_STATUS).value(OrderStatus.PROCESSING.name())
                );
    }

    @Test
    void createOrderWhenItemNotFoundIntegrationTest() throws Exception {
        OrderItemRequestDto item = OrderItemRequestDto.builder()
                .itemId(TestConstant.INTEGER_ID)
                .quantity(TestConstant.ITEM_QUANTITY)
                .build();
        OrderRequestDto requestDto = OrderRequestDto.builder()
                .items(List.of(item))
                .build();

        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_USER_WITH_PREFIX);

        mockMvc.perform(post(INTERNAL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.NOT_FOUND.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString("Item not found with id")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString(String.valueOf(TestConstant.INTEGER_ID))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void createOrderWhenRequestIsIncorrectIntegrationTest() throws Exception {
        OrderRequestDto requestDto = OrderRequestDto.builder().build();

        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_USER_WITH_PREFIX);

        mockMvc.perform(post(INTERNAL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.BAD_REQUEST.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value("Validation failed"),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_DETAILS,
                                hasItem(containsString("Items list is required"))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void createOrderWhenUserDoesNotHaveRightsIntegrationTest() throws Exception {
        OrderItemRequestDto orderItem = OrderItemRequestDto.builder()
                .itemId(TestConstant.INTEGER_ID)
                .quantity(TestConstant.ITEM_QUANTITY)
                .build();
        OrderRequestDto requestDto = OrderRequestDto.builder()
                .items(List.of(orderItem))
                .build();

        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_ADMIN_WITH_PREFIX);

        mockMvc.perform(post(INTERNAL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isForbidden(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.FORBIDDEN.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value("Access Denied"),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void updateOrderWhenUserIsOwnerIntegrationTest() throws Exception {
        Order orderInDb = saveFullOrderWithItem();
        Item newItem = itemRepository.save(Item.builder()
                .name(TestConstant.NEW_ITEM_NAME)
                .price(TestConstant.NEW_ITEM_PRICE)
                .build());
        OrderItemRequestDto orderItem = OrderItemRequestDto.builder()
                .itemId(newItem.getId())
                .quantity(TestConstant.ITEM_QUANTITY)
                .build();
        OrderRequestDto requestDto = OrderRequestDto.builder()
                .items(List.of(orderItem))
                .build();

        setAuthentication(orderInDb.getUserId(), TestConstant.ROLE_USER_WITH_PREFIX);

        userServiceMock.stubFor(WireMock.get(
                        urlEqualTo(String.format("%s/%d", EXTERNAL_SERVICE_URL, TestConstant.LONG_ID)))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(testCustomer))));

        mockMvc.perform(put(String.format("%s/%d", INTERNAL_URL, orderInDb.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER).exists(),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_NAME).value(TestConstant.USER_NAME),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_SURNAME).value(TestConstant.USER_NAME),
                        jsonPath(TestConstant.JSON_PATH_CUSTOMER_EMAIL).value(TestConstant.USER_EMAIL),
                        jsonPath(TestConstant.JSON_PATH_ITEMS).isArray(),
                        jsonPath(TestConstant.JSON_PATH_ITEMS, hasSize(1)),
                        jsonPath(TestConstant.JSON_PATH_ITEM_NAME).value(TestConstant.NEW_ITEM_NAME),
                        jsonPath(TestConstant.JSON_PATH_ITEM_PRICE).value(TestConstant.NEW_ITEM_PRICE),
                        jsonPath(TestConstant.JSON_PATH_ITEM_QUANTITY).value(TestConstant.ITEM_QUANTITY),
                        jsonPath(TestConstant.JSON_PATH_TOTAL_PRICE)
                                .value(TestConstant.NEW_ITEM_PRICE.multiply(BigDecimal.valueOf(TestConstant.ITEM_QUANTITY))),
                        jsonPath(TestConstant.JSON_PATH_STATUS).value(OrderStatus.PROCESSING.name())
                );
    }

    @Test
    void updateOrderWhenUserIsNotOwnerIntegrationTest() throws Exception {
        Order orderInDb = saveFullOrderWithItem();
        OrderItemRequestDto orderItem = OrderItemRequestDto.builder()
                .itemId(TestConstant.INTEGER_ID)
                .quantity(TestConstant.ITEM_QUANTITY)
                .build();
        OrderRequestDto requestDto = OrderRequestDto.builder()
                .items(List.of(orderItem))
                .build();

        setAuthentication(orderInDb.getUserId() + 1, TestConstant.ROLE_USER_WITH_PREFIX);

        userServiceMock.stubFor(WireMock.get(
                        urlEqualTo(String.format("%s/%d", EXTERNAL_SERVICE_URL, TestConstant.LONG_ID)))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(testCustomer))));

        mockMvc.perform(put(String.format("%s/%d", INTERNAL_URL, orderInDb.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isForbidden(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.FORBIDDEN.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value("Access Denied"),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void updateOrderWhenRequestIsIncorrectIntegrationTest() throws Exception {
        OrderRequestDto requestDto = OrderRequestDto.builder().build();

        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_USER_WITH_PREFIX);

        mockMvc.perform(put(String.format("%s/%d", INTERNAL_URL, TestConstant.LONG_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.BAD_REQUEST.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value("Validation failed"),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_DETAILS,
                                hasItem(containsString("Items list is required"))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void updateOrderWhenUserDoesNotHaveRightsIntegrationTest() throws Exception {
        OrderItemRequestDto orderItem = OrderItemRequestDto.builder()
                .itemId(TestConstant.INTEGER_ID)
                .quantity(TestConstant.ITEM_QUANTITY)
                .build();
        OrderRequestDto requestDto = OrderRequestDto.builder()
                .items(List.of(orderItem))
                .build();

        setAuthentication(TestConstant.LONG_ID, TestConstant.ROLE_ADMIN_WITH_PREFIX);

        mockMvc.perform(put(String.format("%s/%d", INTERNAL_URL, TestConstant.LONG_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isForbidden(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.FORBIDDEN.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value("Access Denied"),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void deleteOrderWhenUserIsOwnerIntegrationTest() throws Exception {
        Order orderInDb = saveFullOrderWithItem();

        setAuthentication(orderInDb.getUserId(), TestConstant.ROLE_USER_WITH_PREFIX);

        mockMvc.perform(delete(String.format("%s/%d", INTERNAL_URL, orderInDb.getId())))
                .andExpect(status().isNoContent());

        Optional<Order> deletedOrder = orderRepository.findOrderById(orderInDb.getId());

        deletedOrder.ifPresent(order -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED));
    }

    @Test
    void deleteOrderWhenUserIsNotOwnerIntegrationTest() throws Exception {
        Order orderInDb = saveFullOrderWithItem();

        setAuthentication(orderInDb.getUserId() + 1, TestConstant.ROLE_USER_WITH_PREFIX);

        mockMvc.perform(delete(String.format("%s/%d", INTERNAL_URL, orderInDb.getId())))
                .andExpectAll(
                        status().isForbidden(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.FORBIDDEN.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value("Access Denied"),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void deleteOrderWhenUserIsAdminIntegrationTest() throws Exception {
        Order orderInDb = saveFullOrderWithItem();

        setAuthentication(orderInDb.getUserId() + 1, TestConstant.ROLE_ADMIN_WITH_PREFIX);

        mockMvc.perform(delete(String.format("%s/%d", INTERNAL_URL, orderInDb.getId())))
                .andExpect(status().isNoContent());

        Optional<Order> deletedOrder = orderRepository.findOrderById(orderInDb.getId());

        assertThat(deletedOrder).isNotNull().isEmpty();
    }

    @Test
    void deleteOrderWhenOrderIsAlreadyCancelledIntegrationTest() throws Exception {
        Order cancelledOrder = orderRepository.save(Order.builder()
                .userId(TestConstant.LONG_ID)
                .status(OrderStatus.CANCELLED)
                .build());

        setAuthentication(cancelledOrder.getUserId(), TestConstant.ROLE_USER_WITH_PREFIX);

        mockMvc.perform(delete(String.format("%s/%d", INTERNAL_URL, cancelledOrder.getId())))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.CONFLICT.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(matchesPattern("Order with id \\d+ is already cancelled")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString(String.valueOf(cancelledOrder.getId()))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void deleteOrderWhenOrderIsCompletedIntegrationTest() throws Exception {
        Order completedOrder = orderRepository.save(Order.builder()
                .userId(TestConstant.LONG_ID)
                .status(OrderStatus.COMPLETED)
                .build());

        setAuthentication(completedOrder.getUserId(), TestConstant.ROLE_USER_WITH_PREFIX);

        mockMvc.perform(delete(String.format("%s/%d", INTERNAL_URL, completedOrder.getId())))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.CONFLICT.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(matchesPattern("Order with id \\d+ is already completed")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString(String.valueOf(completedOrder.getId()))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @NotNull
    private Order saveFullOrderWithItem() {
        Item itemInDb = itemRepository.save(DtoBuilder.buildItem());
        OrderItem testOrderItem = DtoBuilder.buildOrderItem();
        testOrderItem.setItem(itemInDb);
        Order testOrder = DtoBuilder.buildOrder();
        testOrder.setOrderItems(List.of(testOrderItem));
        testOrderItem.setOrder(testOrder);

        return orderRepository.save(testOrder);
    }

    private void setAuthentication(Long longId, String roleUserWithPrefix) {
        Authentication testUserAuth = new UsernamePasswordAuthenticationToken(longId, null,
                List.of(new SimpleGrantedAuthority(roleUserWithPrefix)));
        SecurityContextHolder.getContext().setAuthentication(testUserAuth);
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }

    private static class DtoBuilder {
        private static Order buildOrder() {
            return Order.builder()
                    .userId(TestConstant.LONG_ID)
                    .status(OrderStatus.PROCESSING)
                    .build();
        }

        private static Item buildItem() {
            return Item.builder()
                    .name(TestConstant.ITEM_NAME)
                    .price(TestConstant.ITEM_PRICE)
                    .build();
        }

        private static OrderItem buildOrderItem() {
            return OrderItem.builder()
                    .quantity(TestConstant.ITEM_QUANTITY)
                    .build();
        }
    }
}
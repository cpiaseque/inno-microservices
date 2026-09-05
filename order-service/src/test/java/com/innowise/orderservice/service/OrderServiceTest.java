package com.innowise.orderservice.service;

import com.innowise.orderservice.config.SecurityTestConfig;
import com.innowise.orderservice.exception.OrderStatusException;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.model.dto.CustomerDto;
import com.innowise.orderservice.model.dto.OrderItemRequestDto;
import com.innowise.orderservice.model.dto.OrderItemResponseDto;
import com.innowise.orderservice.model.dto.OrderRequestDto;
import com.innowise.orderservice.model.dto.OrderResponseDto;
import com.innowise.orderservice.model.dto.mapper.OrderItemMapperImpl;
import com.innowise.orderservice.model.dto.mapper.OrderMapperImpl;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import com.innowise.orderservice.model.entity.enums.OrderStatus;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.service.circuitbreaker.UserServiceCircuitBreaker;
import com.innowise.orderservice.service.impl.OrderServiceImpl;
import com.innowise.orderservice.service.producer.OrderEventProducer;
import com.innowise.orderservice.util.TestConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.KafkaException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        OrderServiceImpl.class,
        OrderMapperImpl.class,
        OrderItemMapperImpl.class,
        SecurityTestConfig.class
})
class OrderServiceTest {
    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private ItemRepository itemRepository;

    @MockitoBean
    private UserServiceCircuitBreaker circuitBreaker;

    @MockitoBean
    private OrderEventProducer orderEventProducer;

    @Autowired
    private OrderService orderService;

    private static OrderRequestDto testRequestDto;
    private static Order testOrder;
    private static Item testItem;
    private static CustomerDto testCustomer;

    @BeforeAll
    static void beforeAll() {
        testRequestDto = OrderRequestDto.builder()
                .items(List.of(OrderItemRequestDto.builder()
                        .itemId(TestConstant.INTEGER_ID)
                        .build()))
                .build();
        testOrder = Order.builder()
                .id(TestConstant.LONG_ID)
                .userId(TestConstant.LONG_ID)
                .status(OrderStatus.PROCESSING)
                .creationDate(TestConstant.LOCAL_DATE_TIME_NOW)
                .build();
        testItem = Item.builder()
                .id(TestConstant.INTEGER_ID)
                .name(TestConstant.ITEM_NAME)
                .price(BigDecimal.TEN)
                .build();
        OrderItem testOrderItem = OrderItem.builder()
                .order(testOrder)
                .item(testItem)
                .quantity(TestConstant.ITEM_QUANTITY)
                .build();
        testOrder.setOrderItems(List.of(testOrderItem));
        testCustomer = CustomerDto.builder()
                .userId(TestConstant.LONG_ID)
                .name(TestConstant.USER_NAME)
                .surname(TestConstant.USER_NAME)
                .email(TestConstant.USER_EMAIL)
                .build();
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void getOrderByIdWhenUserIsOwnerTest() {
        when(orderRepository.existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID))
                .thenReturn(true);
        when(orderRepository.findOrderById(TestConstant.LONG_ID)).thenReturn(Optional.of(testOrder));
        when(circuitBreaker.getCustomerInfoOrFallback(TestConstant.LONG_ID)).thenReturn(testCustomer);

        OrderResponseDto resultDto = orderService.getOrderById(TestConstant.LONG_ID, TestConstant.LONG_ID);

        assertOrderResponseDtoFields(resultDto, testCustomer);

        verify(orderRepository, times(1)).existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID);
        verify(orderRepository, times(1)).findOrderById(TestConstant.LONG_ID);
        verify(circuitBreaker, times(1)).getCustomerInfoOrFallback(TestConstant.LONG_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void getOrderByIdWhenUserIsNotOwnerTest() {
        when(orderRepository.existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID)).thenReturn(false);

        assertThatThrownBy(() -> orderService.getOrderById(TestConstant.LONG_ID, TestConstant.LONG_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access Denied");

        verify(orderRepository, times(1)).existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID);
        verify(orderRepository, never()).findOrderById(TestConstant.LONG_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_ADMIN_WITHOUT_PREFIX)
    void getOrderByIdWhenUserIsAdminTest() {
        Long adminId = 2L;

        when(orderRepository.findOrderById(TestConstant.LONG_ID)).thenReturn(Optional.of(testOrder));
        when(circuitBreaker.getCustomerInfoOrFallback(TestConstant.LONG_ID)).thenReturn(testCustomer);

        OrderResponseDto resultDto = orderService.getOrderById(TestConstant.LONG_ID, adminId);

        assertOrderResponseDtoFields(resultDto, testCustomer);

        verify(orderRepository, times(1)).findOrderById(TestConstant.LONG_ID);
        verify(circuitBreaker, times(1)).getCustomerInfoOrFallback(TestConstant.LONG_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_ADMIN_WITHOUT_PREFIX)
    void getOrderByIdWhenOrderDoesNotExistTest() {
        when(orderRepository.findOrderById(TestConstant.LONG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(TestConstant.LONG_ID, TestConstant.LONG_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found")
                .hasMessageContaining(String.valueOf(TestConstant.LONG_ID));

        verify(orderRepository, times(1)).findOrderById(TestConstant.LONG_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_ADMIN_WITHOUT_PREFIX)
    void getOrdersByIdsWhenOrderExistsTest() {
        when(orderRepository.findOrdersByIdIn(TestConstant.LONG_IDS)).thenReturn(List.of(testOrder));
        when(circuitBreaker.getCustomersInfoOrFallbackMap(TestConstant.LONG_IDS))
                .thenReturn(Map.of(TestConstant.LONG_ID, testCustomer));

        List<OrderResponseDto> resultList = orderService.getOrdersByIds(TestConstant.LONG_IDS);

        assertThat(resultList).isNotNull().isNotEmpty().hasSize(1);

        OrderResponseDto resultDto = resultList.get(0);

        assertOrderResponseDtoFields(resultDto, testCustomer);

        verify(orderRepository, times(1)).findOrdersByIdIn(TestConstant.LONG_IDS);
        verify(circuitBreaker, times(1)).getCustomersInfoOrFallbackMap(TestConstant.LONG_IDS);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_ADMIN_WITHOUT_PREFIX)
    void getOrdersByIdsWhenOrdersDoesNotExistTest() {
        when(orderRepository.findOrdersByIdIn(TestConstant.LONG_IDS)).thenReturn(Collections.emptyList());

        List<OrderResponseDto> resultList = orderService.getOrdersByIds(TestConstant.LONG_IDS);

        assertThat(resultList).isNotNull().isEmpty();

        verify(orderRepository, times(1)).findOrdersByIdIn(TestConstant.LONG_IDS);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_ADMIN_WITHOUT_PREFIX)
    void getOrdersByStatusesWhenOrderExistsTest() {
        List<OrderStatus> requestStatuses = List.of(OrderStatus.PROCESSING);

        when(orderRepository.findOrdersByStatusIn(requestStatuses)).thenReturn(List.of(testOrder));
        when(circuitBreaker.getCustomersInfoOrFallbackMap(TestConstant.LONG_IDS))
                .thenReturn(Map.of(TestConstant.LONG_ID, testCustomer));

        List<OrderResponseDto> resultList = orderService.getOrdersByStatuses(requestStatuses);

        assertThat(resultList).isNotNull().isNotEmpty().hasSize(1);

        OrderResponseDto resultDto = resultList.get(0);

        assertOrderResponseDtoFields(resultDto, testCustomer);

        verify(orderRepository, times(1)).findOrdersByStatusIn(requestStatuses);
        verify(circuitBreaker, times(1)).getCustomersInfoOrFallbackMap(TestConstant.LONG_IDS);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_ADMIN_WITHOUT_PREFIX)
    void getOrdersByStatusesWhenOrdersDoesNotExistTest() {
        List<OrderStatus> requestStatuses = List.of(OrderStatus.COMPLETED);

        when(orderRepository.findOrdersByStatusIn(requestStatuses)).thenReturn(Collections.emptyList());

        List<OrderResponseDto> resultList = orderService.getOrdersByStatuses(requestStatuses);

        assertThat(resultList).isNotNull().isEmpty();

        verify(orderRepository, times(1)).findOrdersByStatusIn(requestStatuses);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void createOrderSuccessfulTest() {
        when(itemRepository.findItemById(TestConstant.INTEGER_ID)).thenReturn(Optional.of(testItem));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(orderEventProducer).sendOrderEvent(any(Order.class));
        when(circuitBreaker.getCustomerInfoOrFallback(TestConstant.LONG_ID)).thenReturn(testCustomer);

        OrderResponseDto resultDto = orderService.createOrder(TestConstant.LONG_ID, testRequestDto);

        assertOrderResponseDtoFields(resultDto, testCustomer);

        verify(itemRepository, times(1)).findItemById(TestConstant.INTEGER_ID);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderEventProducer, times(1)).sendOrderEvent(any(Order.class));
        verify(circuitBreaker, times(1)).getCustomerInfoOrFallback(TestConstant.LONG_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void createOrderWhenItemNotFoundTest() {
        when(itemRepository.findItemById(TestConstant.INTEGER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(TestConstant.LONG_ID, testRequestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Item not found")
                .hasMessageContaining(String.valueOf(TestConstant.INTEGER_ID));

        verify(itemRepository, times(1)).findItemById(TestConstant.INTEGER_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void createOrderWhenKafkaIsUnavailableTest() {
        when(itemRepository.findItemById(TestConstant.INTEGER_ID)).thenReturn(Optional.of(testItem));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doThrow(new KafkaException("Kafka is unavailable")).when(orderEventProducer).sendOrderEvent(testOrder);

        assertThatThrownBy(() -> orderService.createOrder(TestConstant.LONG_ID, testRequestDto))
                .isInstanceOf(KafkaException.class)
                .hasMessageContaining("Kafka is unavailable");

        verify(itemRepository, times(1)).findItemById(TestConstant.INTEGER_ID);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderEventProducer, times(1)).sendOrderEvent(any(Order.class));
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void updateOrderWithNewItemTest() {
        Order orderInDb = Order.builder()
                .userId(TestConstant.LONG_ID)
                .status(OrderStatus.PROCESSING)
                .orderItems(new ArrayList<>())
                .build();
        orderInDb.getOrderItems().add(OrderItem.builder().build());

        when(orderRepository.existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID)).thenReturn(true);
        when(orderRepository.findOrderById(TestConstant.LONG_ID)).thenReturn(Optional.of(orderInDb));
        when(itemRepository.findItemById(TestConstant.INTEGER_ID)).thenReturn(Optional.of(testItem));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(orderEventProducer).sendOrderEvent(any(Order.class));
        when(circuitBreaker.getCustomerInfoOrFallback(TestConstant.LONG_ID)).thenReturn(testCustomer);

        OrderResponseDto resultDto = orderService.updateOrder(TestConstant.LONG_ID, testRequestDto, TestConstant.LONG_ID);

        assertOrderResponseDtoFields(resultDto, testCustomer);

        verify(orderRepository, times(1)).existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID);
        verify(orderRepository, times(1)).findOrderById(TestConstant.LONG_ID);
        verify(itemRepository, times(1)).findItemById(TestConstant.INTEGER_ID);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderEventProducer, times(1)).sendOrderEvent(any(Order.class));
        verify(circuitBreaker, times(1)).getCustomerInfoOrFallback(TestConstant.LONG_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void updateOrderWhenOrderDoesNotExistTest() {
        when(orderRepository.existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID)).thenReturn(true);
        when(orderRepository.findOrderById(TestConstant.LONG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrder(TestConstant.LONG_ID, testRequestDto, TestConstant.LONG_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found")
                .hasMessageContaining(String.valueOf(TestConstant.LONG_ID));

        verify(orderRepository, times(1)).existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID);
        verify(orderRepository, times(1)).findOrderById(TestConstant.LONG_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void updateOrderWhenUserIsNotOwnerTest() {
        when(orderRepository.existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID)).thenReturn(false);

        assertThatThrownBy(() -> orderService.updateOrder(TestConstant.LONG_ID, testRequestDto, TestConstant.LONG_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access Denied");

        verify(orderRepository, times(1)).existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID);
        verify(orderRepository, never()).findOrderById(TestConstant.LONG_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void updateOrderWhenOrderIsCompletedTest() {
        Order orderInDb = Order.builder()
                .userId(TestConstant.LONG_ID)
                .status(OrderStatus.COMPLETED)
                .orderItems(new ArrayList<>())
                .build();

        when(orderRepository.existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID)).thenReturn(true);
        when(orderRepository.findOrderById(TestConstant.LONG_ID)).thenReturn(Optional.of(orderInDb));

        assertThatThrownBy(() -> orderService.updateOrder(TestConstant.LONG_ID, testRequestDto, TestConstant.LONG_ID))
                .isInstanceOf(OrderStatusException.class)
                .hasMessageMatching("Order with id \\d+ is already completed")
                .hasMessageContaining(String.valueOf(TestConstant.LONG_ID));

        verify(orderRepository, times(1)).existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID);
        verify(orderRepository, times(1)).findOrderById(TestConstant.LONG_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void updateOrderWhenItemNotFoundTest() {
        Order orderInDb = Order.builder()
                .userId(TestConstant.LONG_ID)
                .status(OrderStatus.PROCESSING)
                .orderItems(new ArrayList<>())
                .build();
        orderInDb.getOrderItems().add(OrderItem.builder().build());

        when(orderRepository.existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID)).thenReturn(true);
        when(orderRepository.findOrderById(TestConstant.LONG_ID)).thenReturn(Optional.of(orderInDb));
        when(itemRepository.findItemById(TestConstant.INTEGER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrder(TestConstant.LONG_ID, testRequestDto, TestConstant.LONG_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Item not found")
                .hasMessageContaining(String.valueOf(TestConstant.INTEGER_ID));

        verify(orderRepository, times(1)).existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID);
        verify(orderRepository, times(1)).findOrderById(TestConstant.LONG_ID);
        verify(itemRepository, times(1)).findItemById(TestConstant.INTEGER_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void updateOrderWhenKafkaIsUnavailableTest() {
        Order orderInDb = Order.builder()
                .userId(TestConstant.LONG_ID)
                .status(OrderStatus.PROCESSING)
                .orderItems(new ArrayList<>())
                .build();
        orderInDb.getOrderItems().add(OrderItem.builder().build());

        when(orderRepository.existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID)).thenReturn(true);
        when(orderRepository.findOrderById(TestConstant.LONG_ID)).thenReturn(Optional.of(orderInDb));
        when(itemRepository.findItemById(TestConstant.INTEGER_ID)).thenReturn(Optional.of(testItem));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doThrow(new KafkaException("Kafka is unavailable")).when(orderEventProducer).sendOrderEvent(testOrder);

        assertThatThrownBy(() -> orderService.updateOrder(TestConstant.LONG_ID, testRequestDto, TestConstant.LONG_ID))
                .isInstanceOf(KafkaException.class)
                .hasMessageContaining("Kafka is unavailable");

        verify(orderRepository, times(1)).existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID);
        verify(orderRepository, times(1)).findOrderById(TestConstant.LONG_ID);
        verify(itemRepository, times(1)).findItemById(TestConstant.INTEGER_ID);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderEventProducer, times(1)).sendOrderEvent(any(Order.class));
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void cancelOrderAsUserWhenUserIsOwnerTest() {
        when(orderRepository.existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID)).thenReturn(true);
        when(orderRepository.findOrderById(TestConstant.LONG_ID)).thenReturn(Optional.of(testOrder));

        orderService.cancelOrderAsUser(TestConstant.LONG_ID, TestConstant.LONG_ID);

        verify(orderRepository, times(1)).existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID);
        verify(orderRepository, times(1)).findOrderById(TestConstant.LONG_ID);
        verify(orderRepository, times(1)).cancelOrderAsUser(TestConstant.LONG_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void deleteOrderAsUserWhenUserIsNotOwnerTest() {
        when(orderRepository.existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID)).thenReturn(false);

        assertThatThrownBy(() -> orderService.cancelOrderAsUser(TestConstant.LONG_ID, TestConstant.LONG_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access Denied");

        verify(orderRepository, times(1)).existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID);
        verify(orderRepository, never()).findOrderById(TestConstant.LONG_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void deleteOrderAsUserWhenOrderIsAlreadyCancelledTest() {
        Order cancelledOrder = Order.builder()
                .userId(TestConstant.LONG_ID)
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID)).thenReturn(true);
        when(orderRepository.findOrderById(TestConstant.LONG_ID)).thenReturn(Optional.of(cancelledOrder));

        assertThatThrownBy(() -> orderService.cancelOrderAsUser(TestConstant.LONG_ID, TestConstant.LONG_ID))
                .isInstanceOf(OrderStatusException.class)
                .hasMessageMatching("Order with id \\d+ is already cancelled")
                .hasMessageContaining(String.valueOf(TestConstant.LONG_ID));

        verify(orderRepository, times(1)).existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID);
        verify(orderRepository, times(1)).findOrderById(TestConstant.LONG_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void deleteOrderAsUserWhenOrderIsCompletedTest() {
        Order completedOrder = Order.builder()
                .userId(TestConstant.LONG_ID)
                .status(OrderStatus.COMPLETED)
                .build();

        when(orderRepository.existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID)).thenReturn(true);
        when(orderRepository.findOrderById(TestConstant.LONG_ID)).thenReturn(Optional.of(completedOrder));

        assertThatThrownBy(() -> orderService.cancelOrderAsUser(TestConstant.LONG_ID, TestConstant.LONG_ID))
                .isInstanceOf(OrderStatusException.class)
                .hasMessageMatching("Order with id \\d+ is already completed")
                .hasMessageContaining(String.valueOf(TestConstant.LONG_ID));

        verify(orderRepository, times(1)).existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID);
        verify(orderRepository, times(1)).findOrderById(TestConstant.LONG_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_USER_WITHOUT_PREFIX)
    void deleteOrderWhenOrderDoesNotExistTest() {
        when(orderRepository.existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID)).thenReturn(true);
        when(orderRepository.findOrderById(TestConstant.LONG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrderAsUser(TestConstant.LONG_ID, TestConstant.LONG_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found")
                .hasMessageContaining(String.valueOf(TestConstant.LONG_ID));

        verify(orderRepository, times(1)).existsOrderByIdAndUserId(TestConstant.LONG_ID, TestConstant.LONG_ID);
        verify(orderRepository, times(1)).findOrderById(TestConstant.LONG_ID);
    }

    @Test
    @WithMockUser(roles = TestConstant.ROLE_ADMIN_WITHOUT_PREFIX)
    void deleteOrdersAsAdminSuccessfulTest() {
        Order cancelledOrder = Order.builder()
                .userId(TestConstant.LONG_ID)
                .status(OrderStatus.CANCELLED)
                .build();
        Order completedOrder = Order.builder()
                .userId(TestConstant.LONG_ID)
                .status(OrderStatus.COMPLETED)
                .build();
        List<Order> ordersToDelete = List.of(testOrder, cancelledOrder, completedOrder);

        for (Order order : ordersToDelete) {
            when(orderRepository.findOrderById(TestConstant.LONG_ID)).thenReturn(Optional.of(order));

            orderService.deleteOrderAsAdmin(TestConstant.LONG_ID);
        }

        verify(orderRepository, times(3)).findOrderById(TestConstant.LONG_ID);
        verify(orderRepository, times(3)).delete(any(Order.class));
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(orderRepository, itemRepository, orderEventProducer, circuitBreaker);
    }

    private void assertOrderResponseDtoFields(OrderResponseDto responseDto, CustomerDto expectedCustomer) {
        assertAll(
                () -> assertThat(responseDto).isNotNull(),
                () -> assertThat(responseDto.getCustomer()).isNotNull().isEqualTo(expectedCustomer),
                () -> assertThat(responseDto.getStatus()).isNotNull().isEqualTo(OrderStatus.PROCESSING)
        );

        List<OrderItemResponseDto> receivedItems = responseDto.getItems();

        assertAll(
                () -> assertThat(receivedItems).isNotNull().isNotEmpty().hasSize(1),
                () -> assertThat(receivedItems.get(0).getPrice()).isNotNull().isEqualTo(BigDecimal.TEN),
                () -> assertThat(receivedItems.get(0).getQuantity()).isNotNull().isEqualTo(TestConstant.ITEM_QUANTITY)
        );

        BigDecimal expectedTotalPrice = receivedItems.get(0).getPrice()
                .multiply(BigDecimal.valueOf(receivedItems.get(0).getQuantity()));

        assertThat(responseDto.getTotalPrice()).isNotNull().isEqualTo(expectedTotalPrice);
    }
}
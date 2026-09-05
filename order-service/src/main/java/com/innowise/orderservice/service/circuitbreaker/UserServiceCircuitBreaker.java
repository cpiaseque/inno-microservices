package com.innowise.orderservice.service.circuitbreaker;

import com.innowise.orderservice.client.UserServiceClient;
import com.innowise.orderservice.model.dto.CustomerDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserServiceCircuitBreaker {
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final UserServiceClient userServiceClient;
    private CircuitBreaker circuitBreaker;

    @PostConstruct
    public void init() {
        this.circuitBreaker = circuitBreakerFactory.create("userService");
    }

    public CustomerDto getCustomerInfoOrFallback(Long userId) {
        return circuitBreaker.run(
                () -> userServiceClient.getUserById(userId),
                throwable -> buildUnavailableCustomer(userId)
        );
    }

    public Map<Long, CustomerDto> getCustomersInfoOrFallbackMap(List<Long> userIds) {
        return circuitBreaker.run(
                () -> userServiceClient.getUsersByIds(userIds).stream()
                        .collect(Collectors.toMap(CustomerDto::getUserId, Function.identity())),
                throwable -> userIds.stream()
                        .collect(Collectors.toMap(Function.identity(), this::buildUnavailableCustomer))
        );
    }

    private CustomerDto buildUnavailableCustomer(Long userId) {
        return CustomerDto.builder()
                .userId(userId)
                .errorMessage("User information temporary unavailable")
                .build();
    }
}
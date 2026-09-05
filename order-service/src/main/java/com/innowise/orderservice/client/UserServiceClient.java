package com.innowise.orderservice.client;

import com.innowise.orderservice.config.ServiceFeignConfig;
import com.innowise.orderservice.model.dto.CustomerDto;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "user-service",
        configuration = ServiceFeignConfig.class
)
public interface UserServiceClient {
    @GetMapping("/users/{id}")
    CustomerDto getUserById(@PathVariable("id") Long id);

    @GetMapping(path = "/users", params = "ids")
    List<CustomerDto> getUsersByIds(@RequestParam @NotEmpty List<Long> ids);
}
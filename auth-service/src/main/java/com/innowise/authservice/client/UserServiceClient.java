package com.innowise.authservice.client;

import com.innowise.authservice.config.ServiceFeignConfig;
import com.innowise.authservice.model.dto.RegisterRequestDto;
import com.innowise.authservice.model.dto.RegisterDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "user-service",
        configuration = ServiceFeignConfig.class
)
public interface UserServiceClient {
    @PostMapping("/users")
    RegisterDto createUser(@RequestBody RegisterRequestDto requestDto);
}
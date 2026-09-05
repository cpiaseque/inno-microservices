package com.innowise.authservice.config;

import com.innowise.authservice.model.entity.enums.Role;
import com.innowise.securitystarter.jwt.JwtProvider;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceFeignConfig {
    private final JwtProvider jwtProvider;

    @Bean
    public RequestInterceptor serviceRequestInterceptor() {
        return requestTemplate -> {
            String serviceToken = jwtProvider.generateAccessToken(null, 0L, Role.SERVICE.name());
            requestTemplate.header("Authorization", "Bearer %s".formatted(serviceToken));
        };
    }
}
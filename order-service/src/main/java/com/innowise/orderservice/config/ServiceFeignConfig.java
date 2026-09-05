package com.innowise.orderservice.config;

import com.innowise.orderservice.util.Constant;
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
            String serviceToken = jwtProvider.generateAccessToken(null, 0L, Constant.ROLE_SERVICE);
            requestTemplate.header("Authorization", "Bearer %s".formatted(serviceToken));
        };
    }
}
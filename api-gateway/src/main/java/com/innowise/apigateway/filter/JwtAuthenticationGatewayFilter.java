package com.innowise.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.apigateway.dto.ErrorResponseDto;
import com.innowise.securitystarter.jwt.JwtProvider;
import com.innowise.securitystarter.util.SecurityConstant;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Gateway filter for JWT authentication.
 * Checks JWT tokens for protected routes and allows public paths without authentication.
 */
@Setter
@Component
@ConfigurationProperties(prefix = "gateway.security")
public class JwtAuthenticationGatewayFilter extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilter.Config> {
    private final JwtProvider jwtProvider;
    private List<String> publicPaths = new ArrayList<>();

    public JwtAuthenticationGatewayFilter(JwtProvider jwtProvider) {
        super(Config.class);
        this.jwtProvider = jwtProvider;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();

            if (isPublicEndpoint(path)) {
                return chain.filter(exchange);
            }

            String token = resolveToken(exchange);

            if (token == null || !jwtProvider.validateToken(token)) {
                return handleAuthException(exchange);
            }

            return chain.filter(exchange);
        };
    }

    private boolean isPublicEndpoint(String path) {
        return publicPaths.stream().anyMatch(path::startsWith);
    }

    private String resolveToken(ServerWebExchange exchange) {
        String bearerToken = exchange.getRequest().getHeaders().getFirst(SecurityConstant.AUTH_HEADER);
        if (bearerToken != null && bearerToken.startsWith(SecurityConstant.BEARER_PREFIX)) {
            return bearerToken.substring(SecurityConstant.BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private Mono<Void> handleAuthException(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .errorMessage(SecurityConstant.INVALID_TOKEN_ERROR_MESSAGE)
                .build();

        try {
            byte[] bytes = new ObjectMapper().writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException ex) {
            return Mono.error(ex);
        }
    }

    public static class Config {
    }
}
package com.innowise.securitystarter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for JWT token generation and validation.
 * Configure via application.yml:
 * <pre>{@code
 * jwt:
 *   secret: "your-secret-key"
 *   access-token-expiration: 900000
 *   refresh-token-expiration: 86400000
 *   issuer: "auth-service"
 * }</pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private Long accessTokenExpiration = 3_600_000L; // 1 hour
    private Long refreshTokenExpiration = 604_800_000L; // 1 week
}
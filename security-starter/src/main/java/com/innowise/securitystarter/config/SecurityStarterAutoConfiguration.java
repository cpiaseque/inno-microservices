package com.innowise.securitystarter.config;

import com.innowise.securitystarter.jwt.JwtProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration for JWT security components.
 *
 * @see JwtProvider
 * @see JwtProperties
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityStarterAutoConfiguration {
    @Bean
    public JwtProvider jwtProvider(JwtProperties jwtProperties) {
        return new JwtProvider(jwtProperties);
    }
}
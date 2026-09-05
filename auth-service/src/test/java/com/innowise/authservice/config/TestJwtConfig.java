package com.innowise.authservice.config;

import com.innowise.authservice.util.TokenGenerator;
import com.innowise.securitystarter.config.JwtProperties;
import com.innowise.securitystarter.jwt.JwtProvider;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

@TestConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class TestJwtConfig {
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfig() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-test.yaml"));

        configurer.setProperties(yaml.getObject());
        return configurer;
    }

    @Bean
    public TokenGenerator dtoTokenGenerator(JwtProvider jwtProvider) {
        return new TokenGenerator(jwtProvider);
    }
}
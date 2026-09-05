package com.innowise.userservice.config;

import com.innowise.userservice.model.dto.UserResponseDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class ApplicationConfig {
    @Bean
    public RedisTemplate<String, UserResponseDto> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, UserResponseDto> redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(UserResponseDto.class));
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }
}
package com.innowise.userservice.service.cache;

import com.innowise.userservice.model.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Component responsible for evicting user data from Redis cache.
 * Handles cache invalidation for user operations to ensure data consistency.
 */
@Component
@RequiredArgsConstructor
public class CacheEvictor {
    private final RedisTemplate<String, UserResponseDto> redisTemplate;

    private static final String USER_CACHE_KEY_TEMPLATE = "users::%s";

    /**
     * Evicts cache entries for both user ID and all provided emails in a single Redis operation.
     *
     * @param id the user ID to evict from cache
     * @param emails variable list of email addresses associated with the user
     */
    public void evictUser(Long id, String... emails) {
        Set<String> keys = new HashSet<>();
        keys.add(USER_CACHE_KEY_TEMPLATE.formatted(id));

        for (String email : emails) {
            keys.add(USER_CACHE_KEY_TEMPLATE.formatted(email));
        }

        redisTemplate.delete(keys);
    }
}
package com.innowise.authservice.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for user registration response.
 * Transfers created user identifier from UserService to AuthService.
 */
@Data
@Builder
public class RegisterDto {
    /**
     * Unique user identifier received from UserService.
     */
    private Long userId;
}
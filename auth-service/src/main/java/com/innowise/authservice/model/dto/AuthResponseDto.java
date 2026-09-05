package com.innowise.authservice.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for authentication response.
 * Contains JWT tokens for API access and token refresh.
 */
@Data
@Builder
public class AuthResponseDto {
    /** Short-lived JWT access token for API authorization. */
    private String accessToken;

    /** Long-lived JWT refresh token for obtaining new access tokens. */
    private String refreshToken;
}
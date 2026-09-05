package com.innowise.apigateway.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for standardized error responses for gateway-level authentication.
 * Used by JWT authentication filter.
 */
@Data
@Builder
public class ErrorResponseDto {
    /**
     * HTTP status code of the error response.
     */
    private int status;

    /**
     * Human-readable error message describing the issue.
     */
    private String errorMessage;

    /**
     * Timestamp when the error occurred. Automatically set to current time.
     */
    @Builder.Default
    private String timestamp = LocalDateTime.now().toString();
}
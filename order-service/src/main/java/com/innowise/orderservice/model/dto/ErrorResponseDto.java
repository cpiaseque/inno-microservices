package com.innowise.orderservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.innowise.orderservice.controller.GlobalControllerAdvice;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for standardized error responses.
 * Provides consistent error format across all API endpoints.
 * Used by {@link GlobalControllerAdvice} to handle exceptions.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
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
     * Optional detailed validation errors when multiple field validations fail.
     * Contains specific field errors with descriptions.
     */
    private List<String> errorDetails;

    /**
     * Timestamp when the error occurred. Automatically set to current time.
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
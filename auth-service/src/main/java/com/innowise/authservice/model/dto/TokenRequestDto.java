package com.innowise.authservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for token operations request.
 * Transfers JWT token for validation or refresh operations.
 */
@Data
@Builder
public class TokenRequestDto {
    /** JWT token to validate or refresh. */
    @NotBlank(message = "Token is required")
    private String token;
}
package com.innowise.authservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Data Transfer Object for token validation response.
 * Transfers token validation results and extracted user claims.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponseDto {
    /** Indicates if token has valid signature and structure. */
    private boolean valid;

    /** Contains specific failure reason for invalid tokens, null for valid tokens. */
    private String errorMessage;

    /** User identifier extracted from token claims. */
    private Long userId;

    /** User role extracted from token claims (null for refresh tokens). */
    private String role;
}
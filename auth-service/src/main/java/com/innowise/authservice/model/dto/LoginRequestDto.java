package com.innowise.authservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for user login request.
 * Transfers authentication credentials from client to server.
 */
@Data
@Builder
public class LoginRequestDto {
    /** User's phone number. */
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    /** User's password. */
    @NotBlank(message = "Password is required")
    private String password;
}
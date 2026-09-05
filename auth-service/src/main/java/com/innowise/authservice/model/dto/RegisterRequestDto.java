package com.innowise.authservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Data Transfer Object for user registration request.
 * Transfers user personal information and credentials for account creation.
 */
@Data
@Builder
public class RegisterRequestDto {
    private String name;
    private String surname;
    private LocalDate birthDate;
    private String email;

    /**
     * Phone number in Belarusian format: +375 followed by operator code and 7 digits
     * Supported operator codes: 17, 25, 29, 33, 44.
     */
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+375(17|25|29|33|44)\\d{7}$",
            message = "Phone number must be in format +375XXXXXXXXX with valid operator code")
    private String phoneNumber;

    /** User password with minimum length requirement. */
    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
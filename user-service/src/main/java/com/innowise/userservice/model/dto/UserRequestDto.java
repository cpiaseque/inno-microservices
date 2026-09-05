package com.innowise.userservice.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Data Transfer Object for creating and updating users.
 * Contains validation rules for user input data.
 * Used as request body in user-related API endpoints.
 */
@Data
@Builder
public class UserRequestDto {
    /**
     * User's first name.
     * Must start with capital letter and contain only English letters and hyphens.
     * <p>
     * Examples: "John", "Mary-Jane"
     * </p>
     */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name should be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Z][A-Za-z-]+$",
            message = "Name should start with capital letter and contain only English letters and hyphens")
    private String name;

    /**
     * User's last name (surname).
     * Must start with capital letter and contain only English letters and hyphens.
     * <p>
     * Examples: "Doe", "Smith-Jones"
     * </p>
     */
    @NotBlank(message = "Surname is required")
    @Size(min = 2, max = 50, message = "Surname should be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Z][A-Za-z-]+$",
            message = "Surname should start with capital letter and contain only English letters and hyphens")
    private String surname;

    /**
     * User's date of birth.
     * Must be a date in the past.
     * Format: YYYY-MM-DD
     */
    @NotNull(message = "Date of birth is required")
    @Past(message = "Birth date must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    /**
     * User's email address.
     * Must be a valid email format.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
}
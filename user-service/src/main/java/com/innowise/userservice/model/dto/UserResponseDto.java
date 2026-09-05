package com.innowise.userservice.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object for retrieving users.
 * Contains user data with associated cards as returned from the system.
 * Uses as response body in user-related API endpoints.
 */
@Data
public class UserResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1806232929607712539L;

    /** System-generated unique user identifier. */
    private Long userId;

    /** User's first name. */
    private String name;

    /** User's last name (surname). */
    private String surname;

    /** User's date of birth in YYYY-MM-DD format. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    /** User's email address. */
    private String email;

    /** List of cards associated with this user, empty list if user has no cards */
    private List<CardResponseDto> cards;
}
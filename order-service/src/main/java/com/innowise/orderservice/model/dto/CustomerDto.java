package com.innowise.orderservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Data Transfer Object representing customer information retrieved from UserService.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 3581893990339452228L;

    /**
     * Internal user identifier (write-only and not serialized to JSON responses).
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long userId;

    /**
     * Customer's first name.
     */
    private String name;

    /**
     * Customer's last name (surname).
     */
    private String surname;

    /**
     * Customer's email address.
     */
    private String email;

    /**
     * Error message if customer data is temporary unavailable.
     */
    private String errorMessage;
}
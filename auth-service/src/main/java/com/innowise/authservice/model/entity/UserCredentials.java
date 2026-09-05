package com.innowise.authservice.model.entity;

import com.innowise.authservice.model.entity.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity class which represents user authentication credentials.
 * <p>
 * This class maps to the "user_credentials" table in the database.
 * Contains information for credential management.
 * </p>
 */
@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCredentials {
    /** Primary key representing user's phone number. */
    @Id
    @Column(name = "phone_number")
    private String phoneNumber;

    /**
     * User identifier linking to User entity in separate UserService database.
     * Used for cross-service correlation without foreign key constraints.
     */
    @Column(name = "user_id")
    private Long userId;

    /** BCrypt hashed password for secure authentication. */
    @Column(name = "password")
    private String password;

    /** User role defining access permissions. Automatically defaults to USER if not specified. */
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role;

    /** Timestamp of when the credentials were created. */
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @PrePersist
    public void setDefaultRole() {
        if (role == null) {
            role = Role.USER;
        }
    }
}
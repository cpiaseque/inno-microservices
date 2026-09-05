package com.innowise.userservice.controller;

import com.innowise.userservice.model.dto.UserRequestDto;
import com.innowise.userservice.model.dto.UserResponseDto;
import com.innowise.userservice.exception.EmailAlreadyExistsException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing users in the system.
 * Provides endpoints for CRUD operations on users with role-based authorization.
 *
 * @see UserService
 * @see UserRequestDto
 * @see UserResponseDto
 */
@Validated
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * Retrieves a user by unique identifier.
     * Accessible to ADMIN with full rights, SERVICE for internal communication,
     * and USERS for their own data only.
     *
     * @param id the unique identifier of the user to retrieve
     * @return the user data
     * @throws ResourceNotFoundException if the user with given ID does not exist
     * @throws AccessDeniedException if user does not have permission to access the resource
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE') or (hasRole('USER') and #id == authentication.principal)")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable("id") Long id) {
        UserResponseDto retrievedUser = userService.getUserById(id);

        return ResponseEntity.ok(retrievedUser);
    }

    /**
     * Retrieves a user by email address. Requires ADMIN role.
     *
     * @param email the email address, must be valid and not blank
     * @return the user data
     * @throws ResourceNotFoundException if the user with given email does not exist
     * @throws AccessDeniedException if user does not have ADMIN role
     */
    @GetMapping(params = "email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> getUserByEmail(@RequestParam @NotBlank @Email String email) {
        UserResponseDto retrievedUser = userService.getUserByEmail(email);

        return ResponseEntity.ok(retrievedUser);
    }

    /**
     * Retrieves specific users by their IDs.
     * Accessible to ADMIN with full rights and SERVICE for internal communication.
     *
     * @param ids list of user IDs to filter by
     * @return list of users, empty list if no users found by given IDs
     * @throws AccessDeniedException if user does not have ADMIN role
     */
    @GetMapping(params = "ids")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE')")
    public ResponseEntity<List<UserResponseDto>> getUsersByIds(@RequestParam @NotEmpty List<Long> ids) {
        List<UserResponseDto> retrievedUsers = userService.getUsersByIds(ids);

        return ResponseEntity.ok(retrievedUsers);
    }

    /**
     * Retrieves all users in the system. Requires ADMIN role.
     *
     * @return list of all users, empty list if no users exist
     * @throws AccessDeniedException if user does not have ADMIN role
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDto>> getUsers() {
        List<UserResponseDto> retrievedUsers = userService.getAllUsers();

        return ResponseEntity.ok(retrievedUsers);
    }

    /**
     * Creates a new user with provided data.
     * Accessible only to SERVICE accounts (used by AuthService for user registration).
     *
     * @param userRequestDto the user data to create
     * @return the created user data
     * @throws EmailAlreadyExistsException if email is already registered
     * @throws AccessDeniedException if caller does not have SERVICE role
     */
    @PostMapping
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<UserResponseDto> createUser(@RequestBody @Valid UserRequestDto userRequestDto) {
        UserResponseDto createdUser = userService.createUser(userRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * Updates an existing user's information. Automatically updates holder names in associated cards
     * data if user's name or surname changes.
     * Users can only update their own data unless they have ADMIN role.
     *
     * @param id the unique identifier of the user to update
     * @param userRequestDto the user data to update
     * @return the updated user data
     * @throws ResourceNotFoundException if the user with given ID does not exist
     * @throws EmailAlreadyExistsException if new email is already taken by another user
     * @throws AccessDeniedException if user does not have permission to update the resource
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #id == authentication.principal)")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable("id") Long id,
                                                      @RequestBody @Valid UserRequestDto userRequestDto) {
        UserResponseDto updatedUser = userService.updateUser(id, userRequestDto);

        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Deletes a user by unique identifier. Also removes all associated cards.
     * Users can only delete their own account unless they have ADMIN role.
     *
     * @param id the unique identifier of the user to delete
     * @return empty response
     * @throws ResourceNotFoundException if the user with given ID does not exist
     * @throws AccessDeniedException if user does not have permission to delete the resource
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #id == authentication.principal)")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }
}
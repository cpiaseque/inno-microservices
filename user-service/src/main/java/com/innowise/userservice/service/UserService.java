package com.innowise.userservice.service;

import com.innowise.userservice.model.dto.UserRequestDto;
import com.innowise.userservice.model.dto.UserResponseDto;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.exception.EmailAlreadyExistsException;
import com.innowise.userservice.exception.ResourceNotFoundException;

import java.util.List;

/**
 * Service for managing users in the system.
 * Handles business logic for user operations including email uniqueness verification
 * and cascade updates to associated cards.
 *
 * @see User
 * @see UserRequestDto
 * @see UserResponseDto
 */
public interface UserService {
    /**
     * Retrieves all users from the system.
     *
     * @return list of all users, empty list if no users exist in the database
     */
    List<UserResponseDto> getAllUsers();

    /**
     * Retrieves a user by unique identifier.
     *
     * @param userId the unique identifier of the user to retrieve
     * @return the user data
     * @throws ResourceNotFoundException if the user with given ID does not exist
     */
    UserResponseDto getUserById(Long userId);

    /**
     * Retrieves multiple users by their identifiers.
     * Returns only existing users, non-existent IDs are ignored.
     *
     * @param usersIds the list of user identifiers to retrieve
     * @return list of found users, empty list if no users found by IDs
     */
    List<UserResponseDto> getUsersByIds(List<Long> usersIds);

    /**
     * Retrieves a user by email address.
     *
     * @param email the email address to search for
     * @return the user data
     * @throws ResourceNotFoundException if the user with given email does not exist
     */
    UserResponseDto getUserByEmail(String email);

    /**
     * Creates a new user after verifying email uniqueness.
     *
     * @param userRequestDto the user data to create
     * @return the created user data
     * @throws EmailAlreadyExistsException if email is already registered
     */
    UserResponseDto createUser(UserRequestDto userRequestDto);

    /**
     * Updates an existing user's information after verifying email uniqueness.
     * Automatically updates holder names in associated cards data if user's name or surname changes.
     *
     * @param userId the unique identifier of the user to update
     * @param userRequestDto the user data to update
     * @return the updated user data
     * @throws ResourceNotFoundException if the user with given ID does not exist
     * @throws EmailAlreadyExistsException if new email is already taken by another user
     */
    UserResponseDto updateUser(Long userId, UserRequestDto userRequestDto);

    /**
     * Deletes a user and all associated cards.
     *
     * @param userId the unique identifier of the user to delete
     * @throws ResourceNotFoundException if the user with given ID does not exist
     */
    void deleteUser(Long userId);
}
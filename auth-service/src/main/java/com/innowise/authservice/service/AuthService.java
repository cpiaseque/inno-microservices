package com.innowise.authservice.service;

import com.innowise.authservice.client.UserServiceClient;
import com.innowise.authservice.exception.PhoneNumberAlreadyExistsException;
import com.innowise.authservice.model.dto.AuthResponseDto;
import com.innowise.authservice.model.dto.LoginRequestDto;
import com.innowise.authservice.model.dto.RegisterRequestDto;
import com.innowise.authservice.model.dto.TokenRequestDto;
import com.innowise.authservice.model.dto.TokenResponseDto;
import com.innowise.authservice.model.entity.UserCredentials;
import com.innowise.securitystarter.jwt.JwtProvider;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Service handling authentication and authorization business logic.
 * Provides user registration, login, token validation and refresh capabilities.
 *
 * @see UserCredentials
 * @see AuthResponseDto
 * @see RegisterRequestDto
 * @see LoginRequestDto
 * @see TokenRequestDto
 * @see TokenResponseDto
 * @see UserServiceClient
 * @see JwtProvider
 */
public interface AuthService {
    /**
     * Registers a new user in the system.
     * Creates user profile using UserService and stores authentication credentials.
     * Returns JWT tokens for immediate access after registration.
     *
     * @param registerRequestDto the user registration data
     * @return authentication response with access and refresh tokens
     * @throws PhoneNumberAlreadyExistsException if phone number is already registered
     */
    AuthResponseDto registerUser(RegisterRequestDto registerRequestDto);

    /**
     * Authenticates user with phone number and password.
     * Verifies credentials and returns JWT tokens.
     *
     * @param loginRequestDto the login credentials
     * @return authentication response with access and refresh tokens
     * @throws BadCredentialsException if phone number not found or password doesn't match
     */
    AuthResponseDto authenticateUser(LoginRequestDto loginRequestDto);

    /**
     * Validates JWT token and extracts user information.
     *
     * @param tokenRequestDto the token validation request containing JWT token
     * @return token validation response with validity status and user claims
     */
    TokenResponseDto validateToken(TokenRequestDto tokenRequestDto);

    /**
     * Issues new access and refresh tokens using valid refresh token.
     *
     * @param tokenRequestDto the refresh token request
     * @return new authentication response with fresh tokens
     * @throws BadCredentialsException if refresh token is invalid, expired, or user not found
     */
    AuthResponseDto refreshToken(TokenRequestDto tokenRequestDto);
}
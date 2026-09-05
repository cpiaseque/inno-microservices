package com.innowise.authservice.controller;

import com.innowise.authservice.exception.PhoneNumberAlreadyExistsException;
import com.innowise.authservice.model.dto.AuthResponseDto;
import com.innowise.authservice.model.dto.LoginRequestDto;
import com.innowise.authservice.model.dto.RegisterRequestDto;
import com.innowise.authservice.model.dto.TokenRequestDto;
import com.innowise.authservice.model.dto.TokenResponseDto;
import com.innowise.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication and authorization operations.
 * Provides endpoints for user registration, login, token validation and refresh.
 *
 * @see AuthService
 * @see RegisterRequestDto
 * @see LoginRequestDto
 * @see AuthResponseDto
 * @see TokenRequestDto
 * @see TokenResponseDto
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * Registers a new user in the system. Creates user profile and authentication credentials.
     *
     * @param registerRequestDto the user data to create and register
     * @return generated JWT tokens
     * @throws PhoneNumberAlreadyExistsException if phone number is already registered
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@RequestBody @Valid RegisterRequestDto registerRequestDto) {
        AuthResponseDto authResponseDto = authService.registerUser(registerRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(authResponseDto);
    }

    /**
     * Authenticates user with credentials. Verifies phone number and password.
     *
     * @param loginRequestDto login credentials
     * @return generated JWT tokens
     * @throws BadCredentialsException if authentication fails
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody @Valid LoginRequestDto loginRequestDto) {
        AuthResponseDto authResponseDto = authService.authenticateUser(loginRequestDto);

        return ResponseEntity.ok(authResponseDto);
    }

    /**
     * Validates JWT token and returns its status and claims.
     *
     * @param tokenRequestDto JWT token to validate
     * @return token validation results
     */
    @PostMapping("/validate")
    public ResponseEntity<TokenResponseDto> validate(@RequestBody @Valid TokenRequestDto tokenRequestDto) {
        TokenResponseDto tokenResponseDto = authService.validateToken(tokenRequestDto);

        return ResponseEntity.ok(tokenResponseDto);
    }

    /**
     * Refreshes authentication tokens using valid refresh token.
     *
     * @param tokenRequestDto JWT token to refresh
     * @return generated JWT tokens
     * @throws BadCredentialsException if refresh token is invalid or expired
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@RequestBody @Valid TokenRequestDto tokenRequestDto) {
        AuthResponseDto authResponseDto = authService.refreshToken(tokenRequestDto);

        return ResponseEntity.ok(authResponseDto);
    }
}
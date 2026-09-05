package com.innowise.authservice.service.impl;

import com.innowise.authservice.client.UserServiceClient;
import com.innowise.authservice.exception.PhoneNumberAlreadyExistsException;
import com.innowise.authservice.model.dto.AuthResponseDto;
import com.innowise.authservice.model.dto.LoginRequestDto;
import com.innowise.authservice.model.dto.RegisterRequestDto;
import com.innowise.authservice.model.dto.RegisterDto;
import com.innowise.authservice.model.dto.TokenRequestDto;
import com.innowise.authservice.model.dto.TokenResponseDto;
import com.innowise.authservice.model.entity.UserCredentials;
import com.innowise.authservice.repository.UserCredentialsRepository;
import com.innowise.authservice.service.AuthService;
import com.innowise.securitystarter.jwt.JwtProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserCredentialsRepository credentialsRepository;
    private final UserServiceClient userServiceClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional
    public AuthResponseDto registerUser(RegisterRequestDto registerRequestDto) {
        if (credentialsRepository.existsByPhoneNumber(registerRequestDto.getPhoneNumber())) {
            throw new PhoneNumberAlreadyExistsException(registerRequestDto.getPhoneNumber());
        }

        RegisterDto registerDto = userServiceClient.createUser(registerRequestDto);

        UserCredentials newCredentials = UserCredentials.builder()
                .phoneNumber(registerRequestDto.getPhoneNumber())
                .userId(registerDto.getUserId())
                .password(passwordEncoder.encode(registerRequestDto.getPassword()))
                .build();

        UserCredentials savedCredentials = credentialsRepository.save(newCredentials);

        return generateTokens(savedCredentials);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto authenticateUser(LoginRequestDto loginRequestDto) {
        UserCredentials credentials = credentialsRepository.findCredentialsByPhoneNumber(loginRequestDto.getPhoneNumber())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), credentials.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return generateTokens(credentials);
    }

    @Override
    public TokenResponseDto validateToken(TokenRequestDto tokenRequestDto) {
        String token = tokenRequestDto.getToken();
        TokenResponseDto responseDto = new TokenResponseDto();

        try {
            responseDto.setValid(jwtProvider.validateToken(token));
            extractUserAndRole(token, responseDto);
        } catch (ExpiredJwtException ex) {
            responseDto.setErrorMessage("Token is expired");
            extractUserAndRole(token, responseDto);
        } catch (JwtException ex) {
            responseDto.setErrorMessage("Invalid token: %s".formatted(ex.getMessage()));
        }

        return responseDto;
    }

    private void extractUserAndRole(String token, TokenResponseDto responseDto) {
        responseDto.setUserId(jwtProvider.extractUserId(token));
        responseDto.setRole(jwtProvider.extractRole(token));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto refreshToken(TokenRequestDto tokenRequestDto) {
        String refreshToken = tokenRequestDto.getToken();

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        if (!jwtProvider.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Not a refresh token");
        }

        Long userId = jwtProvider.extractUserId(refreshToken);
        UserCredentials credentials = credentialsRepository.findCredentialsByUserId(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found with id: %d".formatted(userId)));

        return generateTokens(credentials);
    }

    private AuthResponseDto generateTokens(UserCredentials credentials) {
        String accessToken = jwtProvider.generateAccessToken(
                credentials.getPhoneNumber(),
                credentials.getUserId(),
                credentials.getRole().name()
        );
        String refreshToken = jwtProvider.generateRefreshToken(
                credentials.getPhoneNumber(),
                credentials.getUserId()
        );

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
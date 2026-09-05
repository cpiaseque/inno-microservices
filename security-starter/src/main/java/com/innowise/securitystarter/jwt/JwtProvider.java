package com.innowise.securitystarter.jwt;

import com.innowise.securitystarter.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Main component for JWT token operations.
 * Provides methods for token generation, validation and data extraction.
 */
@RequiredArgsConstructor
public class JwtProvider {
    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    private static final String USER_ID_CLAIM = "userId";
    private static final String ROLE_CLAIM = "role";

    /**
     * Lazily initializes and gets the key from configured secret.
     *
     * @return SecretKey instance for HMAC-SHA signing
     */
    private SecretKey getSigningKey() {
        if (secretKey == null) {
            secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        }
        return secretKey;
    }

    /**
     * Generates a JWT access token with user information and role claims.
     *
     * @param phoneNumber the user's phone number (token subject)
     * @param userId the unique user identifier
     * @param role the user's role for authorization
     * @return signed JWT access token
     */
    public String generateAccessToken(String phoneNumber, Long userId, String role) {
        return Jwts.builder()
                .subject(phoneNumber)
                .claim(USER_ID_CLAIM, userId)
                .claim(ROLE_CLAIM, role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generates a JWT refresh token for obtaining new access tokens.
     *
     * @param phoneNumber the user's phone number (token subject)
     * @param userId the unique user identifier
     * @return signed JWT refresh token string
     */
    public String generateRefreshToken(String phoneNumber, Long userId) {
        return Jwts.builder()
                .subject(phoneNumber)
                .claim(USER_ID_CLAIM, userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validates JWT token signature and basic structure.
     *
     * @param token the JWT token to validate
     * @return true if token has valid signature and structure, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Extracts user ID from JWT token claims.
     *
     * @param token the JWT token
     * @return user identifier from token claims
     */
    public Long extractUserId(String token) {
        return extractClaims(token).get(USER_ID_CLAIM, Long.class);
    }

    /**
     * Extracts role from JWT token claims.
     *
     * @param token the JWT token
     * @return user role from token claims, null if not present
     */
    public String extractRole(String token) {
        return extractClaims(token).get(ROLE_CLAIM, String.class);
    }

    /**
     * Determines if the given token is a refresh token (by the absence of role claim).
     *
     * @param token the JWT token to check
     * @return true if token is a refresh token, false if access or invalid token
     */
    public boolean isRefreshToken(String token) {
        try {
            String role = extractClaims(token).get(ROLE_CLAIM, String.class);
            return role == null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Converts JWT token role claim to Spring Security authorities.
     *
     * @param token the JWT token
     * @return list of GrantedAuthority objects with "ROLE_" prefix
     */
    public List<GrantedAuthority> extractAuthorities(String token) {
        String role = extractClaims(token).get(ROLE_CLAIM, String.class);

        if (role == null || role.isBlank()) {
            return Collections.emptyList();
        }

        return List.of(new SimpleGrantedAuthority("ROLE_%s".formatted(role)));
    }

    private Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        }
    }
}
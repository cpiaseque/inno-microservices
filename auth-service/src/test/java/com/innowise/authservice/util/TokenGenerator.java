package com.innowise.authservice.util;

import com.innowise.securitystarter.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenGenerator {
    @Autowired
    private final JwtProvider jwtProvider;

    public String generateAccessToken() {
        return jwtProvider.generateAccessToken(
                TestConstant.PHONE_NUMBER, TestConstant.ID, TestConstant.ROLE_USER.name()
        );
    }

    public String generateRefreshToken() {
        return jwtProvider.generateRefreshToken(TestConstant.PHONE_NUMBER, TestConstant.ID);
    }
}
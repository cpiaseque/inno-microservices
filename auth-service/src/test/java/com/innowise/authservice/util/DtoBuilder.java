package com.innowise.authservice.util;

import com.innowise.authservice.model.dto.LoginRequestDto;
import com.innowise.authservice.model.dto.RegisterDto;
import com.innowise.authservice.model.dto.RegisterRequestDto;
import com.innowise.authservice.model.entity.UserCredentials;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class DtoBuilder {
    private static final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public static RegisterRequestDto buildRegisterRequestDto() {
        return RegisterRequestDto.builder()
                .phoneNumber(TestConstant.PHONE_NUMBER)
                .password(TestConstant.PASSWORD)
                .build();
    }

    public static RegisterDto buildRegisterDto() {
        return RegisterDto.builder()
                .userId(TestConstant.ID)
                .build();
    }

    public static UserCredentials buildUserCredentials() {
        return UserCredentials.builder()
                .userId(TestConstant.ID)
                .phoneNumber(TestConstant.PHONE_NUMBER)
                .password(encoder.encode(TestConstant.PASSWORD))
                .role(TestConstant.ROLE_USER)
                .createdAt(TestConstant.CREATION_TIMESTAMP)
                .build();
    }

    public static LoginRequestDto buildLoginRequestDto(String password) {
        return LoginRequestDto.builder()
                .phoneNumber(TestConstant.PHONE_NUMBER)
                .password(password)
                .build();
    }
}
package com.innowise.authservice.util;

import com.innowise.authservice.model.entity.enums.Role;

import java.time.LocalDateTime;

public final class TestConstant {
    public static final Long ID = 1L;
    public static final String PHONE_NUMBER = "+375251234567";
    public static final String PASSWORD = "TestPassword";
    public static final Role ROLE_USER = Role.USER;
    public static final LocalDateTime CREATION_TIMESTAMP = LocalDateTime.now();

    public static final String WRONG_PASSWORD = "WrongPassword";

    public static final String JSON_PATH_ACCESS_TOKEN = "accessToken";
    public static final String JSON_PATH_REFRESH_TOKEN = "refreshToken";

    public static final String JSON_PATH_EXCEPTION_STATUS = "$.status";
    public static final String JSON_PATH_EXCEPTION_ERROR_MESSAGE = "$.errorMessage";
    public static final String JSON_PATH_EXCEPTION_DETAILS = "$.errorDetails";
    public static final String JSON_PATH_EXCEPTION_TIMESTAMP = "$.timestamp";

    public static final String JSON_PATH_VALID = "valid";
    public static final String JSON_PATH_ERROR_MESSAGE = "errorMessage";
    public static final String JSON_PATH_USER_ID = "userId";
    public static final String JSON_PATH_ROLE = "role";

    private TestConstant() {
    }
}
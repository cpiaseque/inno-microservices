package com.innowise.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.authservice.client.UserServiceClient;
import com.innowise.authservice.model.dto.ErrorResponseDto;
import com.innowise.authservice.model.dto.LoginRequestDto;
import com.innowise.authservice.model.dto.RegisterDto;
import com.innowise.authservice.model.dto.RegisterRequestDto;
import com.innowise.authservice.model.dto.TokenRequestDto;
import com.innowise.authservice.model.entity.UserCredentials;
import com.innowise.authservice.repository.UserCredentialsRepository;
import com.innowise.authservice.util.DtoBuilder;
import com.innowise.authservice.util.TestConstant;
import com.innowise.authservice.util.TokenGenerator;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest {
    @Container
    protected static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @DynamicPropertySource
    protected static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final String URL_FORMATTED = "/auth/%s";
    private static final String REGISTER = "register";
    private static final String LOGIN = "login";
    private static final String VALIDATE = "validate";
    private static final String REFRESH = "refresh";

    @MockitoBean
    private UserServiceClient userServiceClient;

    @Autowired
    private UserCredentialsRepository credentialsRepository;

    @Autowired
    private TokenGenerator tokenGenerator;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerNewUserSuccessfulIntegrationTest() throws Exception {
        RegisterRequestDto requestDto = DtoBuilder.buildRegisterRequestDto();
        RegisterDto innerResponse = DtoBuilder.buildRegisterDto();

        when(userServiceClient.createUser(requestDto)).thenReturn(innerResponse);

        mockMvc.perform(post(URL_FORMATTED.formatted(REGISTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath(TestConstant.JSON_PATH_ACCESS_TOKEN).exists(),
                        jsonPath(TestConstant.JSON_PATH_ACCESS_TOKEN).isString(),
                        jsonPath(TestConstant.JSON_PATH_REFRESH_TOKEN).exists(),
                        jsonPath(TestConstant.JSON_PATH_REFRESH_TOKEN).isString()
                );

        credentialsRepository.deleteAll();
    }

    @Test
    void registerUserWithNotValidFieldsIntegrationTest() throws Exception {
        RegisterRequestDto requestDto = RegisterRequestDto.builder()
                .phoneNumber("123")
                .password("321")
                .build();

        mockMvc.perform(post(URL_FORMATTED.formatted(REGISTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.BAD_REQUEST.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString("Validation failed")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_DETAILS,
                                hasItem(containsString("Phone number must be in format +375XXXXXXXXX with valid operator code"))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_DETAILS,
                                hasItem(containsString("Password must be at least 8 characters"))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void registerUserWhenPhoneNumberAlreadyExistsIntegrationTest() throws Exception {
        UserCredentials existingUser = DtoBuilder.buildUserCredentials();
        credentialsRepository.save(existingUser);
        RegisterRequestDto requestDto = DtoBuilder.buildRegisterRequestDto();

        mockMvc.perform(post(URL_FORMATTED.formatted(REGISTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.CONFLICT.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString("Phone number already exists in the database")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString(TestConstant.PHONE_NUMBER)),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );

        credentialsRepository.deleteAll();
    }

    @Test
    void registerUserWhenUserServiceThrewExceptionIntegrationTest() throws Exception {
        RegisterRequestDto requestDto = DtoBuilder.buildRegisterRequestDto();

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .status(409)
                .errorMessage("Email already exists in the database")
                .timestamp(LocalDateTime.now())
                .build();

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(409);
        when(feignException.contentUTF8()).thenReturn(objectMapper.writeValueAsString(errorResponse));

        when(userServiceClient.createUser(requestDto)).thenThrow(feignException);

        mockMvc.perform(post(URL_FORMATTED.formatted(REGISTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.CONFLICT.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString("Email already exists in the database")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void loginUserSuccessfulIntegrationTest() throws Exception {
        UserCredentials existingUser = DtoBuilder.buildUserCredentials();
        credentialsRepository.save(existingUser);

        LoginRequestDto requestDto = DtoBuilder.buildLoginRequestDto(TestConstant.PASSWORD);

        mockMvc.perform(post(URL_FORMATTED.formatted(LOGIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_ACCESS_TOKEN).exists(),
                        jsonPath(TestConstant.JSON_PATH_ACCESS_TOKEN).isString(),
                        jsonPath(TestConstant.JSON_PATH_REFRESH_TOKEN).exists(),
                        jsonPath(TestConstant.JSON_PATH_REFRESH_TOKEN).isString()
                );

        credentialsRepository.deleteAll();
    }

    @Test
    void loginUserWhenPasswordIsWrongIntegrationTest() throws Exception {
        UserCredentials existingUser = DtoBuilder.buildUserCredentials();
        credentialsRepository.save(existingUser);

        LoginRequestDto requestDto = DtoBuilder.buildLoginRequestDto(TestConstant.WRONG_PASSWORD);

        mockMvc.perform(post(URL_FORMATTED.formatted(LOGIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.UNAUTHORIZED.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString("Invalid credentials")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );

        credentialsRepository.deleteAll();
    }

    @Test
    void loginUserWhenUserDoesNotExistIntegrationTest() throws Exception {
        LoginRequestDto requestDto = DtoBuilder.buildLoginRequestDto(TestConstant.PASSWORD);

        mockMvc.perform(post(URL_FORMATTED.formatted(LOGIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.UNAUTHORIZED.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString("Invalid credentials")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void validateTokenSuccessfulIntegrationTest() throws Exception {
        TokenRequestDto requestDto = TokenRequestDto.builder()
                .token(tokenGenerator.generateAccessToken())
                .build();

        mockMvc.perform(post(URL_FORMATTED.formatted(VALIDATE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_VALID).value(Boolean.TRUE),
                        jsonPath(TestConstant.JSON_PATH_ERROR_MESSAGE).doesNotExist(),
                        jsonPath(TestConstant.JSON_PATH_USER_ID).value(TestConstant.ID),
                        jsonPath(TestConstant.JSON_PATH_ROLE).value(TestConstant.ROLE_USER.name())
                );
    }

    @Test
    void validateInvalidTokenIntegrationTest() throws Exception {
        TokenRequestDto requestDto = TokenRequestDto.builder()
                .token(tokenGenerator.generateAccessToken().toUpperCase())
                .build();

        mockMvc.perform(post(URL_FORMATTED.formatted(VALIDATE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_VALID).value(Boolean.FALSE),
                        jsonPath(TestConstant.JSON_PATH_ERROR_MESSAGE).value(containsString("Invalid token")),
                        jsonPath(TestConstant.JSON_PATH_USER_ID).doesNotExist(),
                        jsonPath(TestConstant.JSON_PATH_ROLE).doesNotExist()
                );
    }

    @Test
    void refreshTokenSuccessfulIntegrationTest() throws Exception {
        UserCredentials existingUser = DtoBuilder.buildUserCredentials();
        credentialsRepository.save(existingUser);

        TokenRequestDto requestDto = TokenRequestDto.builder()
                .token(tokenGenerator.generateRefreshToken())
                .build();

        mockMvc.perform(post(URL_FORMATTED.formatted(REFRESH))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_ACCESS_TOKEN).exists(),
                        jsonPath(TestConstant.JSON_PATH_ACCESS_TOKEN).isString(),
                        jsonPath(TestConstant.JSON_PATH_REFRESH_TOKEN).exists(),
                        jsonPath(TestConstant.JSON_PATH_REFRESH_TOKEN).isString()
                );

        credentialsRepository.deleteAll();
    }

    @Test
    void refreshTokenWhenTheTokenIsNotRefreshIntegrationTest() throws Exception {
        TokenRequestDto requestDto = TokenRequestDto.builder()
                .token(tokenGenerator.generateAccessToken())
                .build();

        mockMvc.perform(post(URL_FORMATTED.formatted(REFRESH))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.UNAUTHORIZED.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString("Not a refresh token")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void refreshTokenWhenUserNotFoundIntegrationTest() throws Exception {
        TokenRequestDto requestDto = TokenRequestDto.builder()
                .token(tokenGenerator.generateRefreshToken())
                .build();

        mockMvc.perform(post(URL_FORMATTED.formatted(REFRESH))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.UNAUTHORIZED.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString("User not found")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE)
                                .value(containsString(String.valueOf(TestConstant.ID))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }
}
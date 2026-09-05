package com.innowise.authservice.service;

import com.innowise.authservice.client.UserServiceClient;
import com.innowise.authservice.config.SecurityConfig;
import com.innowise.authservice.config.TestJwtConfig;
import com.innowise.authservice.exception.PhoneNumberAlreadyExistsException;
import com.innowise.authservice.model.dto.AuthResponseDto;
import com.innowise.authservice.model.dto.LoginRequestDto;
import com.innowise.authservice.model.dto.RegisterDto;
import com.innowise.authservice.model.dto.RegisterRequestDto;
import com.innowise.authservice.model.dto.TokenRequestDto;
import com.innowise.authservice.model.dto.TokenResponseDto;
import com.innowise.authservice.model.entity.UserCredentials;
import com.innowise.authservice.repository.UserCredentialsRepository;
import com.innowise.authservice.service.impl.AuthServiceImpl;
import com.innowise.authservice.util.DtoBuilder;
import com.innowise.authservice.util.TokenGenerator;
import com.innowise.authservice.util.TestConstant;
import com.innowise.securitystarter.config.JwtProperties;
import com.innowise.securitystarter.jwt.JwtProvider;
import feign.FeignException;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        AuthServiceImpl.class,
        SecurityConfig.class,
        JwtProperties.class,
        JwtProvider.class,
        TestJwtConfig.class
})
public class AuthServiceTest {
    @MockitoBean
    private UserCredentialsRepository credentialsRepository;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private TokenGenerator tokenGenerator;

    @Autowired
    private AuthService authService;

    @Test
    void registerUserSuccessfulTest() {
        RegisterRequestDto registerRequestDto = DtoBuilder.buildRegisterRequestDto();
        RegisterDto registerDto = DtoBuilder.buildRegisterDto();
        UserCredentials userCredentials = DtoBuilder.buildUserCredentials();

        when(credentialsRepository.existsByPhoneNumber(TestConstant.PHONE_NUMBER)).thenReturn(false);
        when(userServiceClient.createUser(registerRequestDto)).thenReturn(registerDto);
        when(credentialsRepository.save(any(UserCredentials.class))).thenReturn(userCredentials);

        AuthResponseDto authResponse = authService.registerUser(registerRequestDto);

        assertGeneratedTokens(authResponse);

        verify(credentialsRepository, times(1)).existsByPhoneNumber(TestConstant.PHONE_NUMBER);
        verify(userServiceClient, times(1)).createUser(registerRequestDto);
        verify(credentialsRepository, times(1)).save(any(UserCredentials.class));
    }

    @Test
    void registerUserWhenPhoneNumberAlreadyExistsTest() {
        RegisterRequestDto registerRequestDto = DtoBuilder.buildRegisterRequestDto();

        when(credentialsRepository.existsByPhoneNumber(TestConstant.PHONE_NUMBER)).thenReturn(true);

        assertThatThrownBy(() -> authService.registerUser(registerRequestDto))
                .isInstanceOf(PhoneNumberAlreadyExistsException.class)
                .hasMessageContaining("Phone number already exists in the database")
                .hasMessageContaining(TestConstant.PHONE_NUMBER);

        verify(credentialsRepository, times(1)).existsByPhoneNumber(TestConstant.PHONE_NUMBER);
    }

    @Test
    void registerUserWhenUserServiceThrewExceptionTest() {
        RegisterRequestDto registerRequestDto = DtoBuilder.buildRegisterRequestDto();

        when(credentialsRepository.existsByPhoneNumber(TestConstant.PHONE_NUMBER)).thenReturn(false);
        when(userServiceClient.createUser(registerRequestDto)).thenThrow(FeignException.class);

        assertThatThrownBy(() -> authService.registerUser(registerRequestDto))
                .isInstanceOf(FeignException.class);

        verify(credentialsRepository, times(1)).existsByPhoneNumber(TestConstant.PHONE_NUMBER);
        verify(userServiceClient, times(1)).createUser(registerRequestDto);
    }

    @Test
    void authenticateUserSuccessfulTest() {
        LoginRequestDto loginRequestDto = DtoBuilder.buildLoginRequestDto(TestConstant.PASSWORD);
        UserCredentials userCredentials = DtoBuilder.buildUserCredentials();

        when(credentialsRepository.findCredentialsByPhoneNumber(TestConstant.PHONE_NUMBER))
                .thenReturn(Optional.of(userCredentials));

        AuthResponseDto authResponseDto = authService.authenticateUser(loginRequestDto);

        assertGeneratedTokens(authResponseDto);

        verify(credentialsRepository, times(1)).findCredentialsByPhoneNumber(TestConstant.PHONE_NUMBER);
    }

    @Test
    void authenticateUserWhenUserNotRegisteredTest() {
        LoginRequestDto loginRequestDto = DtoBuilder.buildLoginRequestDto(TestConstant.PASSWORD);

        when(credentialsRepository.findCredentialsByPhoneNumber(TestConstant.PHONE_NUMBER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticateUser(loginRequestDto))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid credentials");

        verify(credentialsRepository, times(1)).findCredentialsByPhoneNumber(TestConstant.PHONE_NUMBER);
    }

    @Test
    void authenticateUserWhenPasswordIsWrongTest() {
        LoginRequestDto loginRequestDto = DtoBuilder.buildLoginRequestDto(TestConstant.WRONG_PASSWORD);
        UserCredentials userCredentials = DtoBuilder.buildUserCredentials();

        when(credentialsRepository.findCredentialsByPhoneNumber(TestConstant.PHONE_NUMBER))
                .thenReturn(Optional.of(userCredentials));

        assertThatThrownBy(() -> authService.authenticateUser(loginRequestDto))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid credentials");

        verify(credentialsRepository, times(1)).findCredentialsByPhoneNumber(TestConstant.PHONE_NUMBER);
    }

    @Test
    void validateCorrectTokenTest() {
        TokenRequestDto requestDto = TokenRequestDto.builder()
                .token(tokenGenerator.generateAccessToken())
                .build();

        TokenResponseDto responseDto = authService.validateToken(requestDto);

        assertAll(
                () -> assertThat(responseDto).isNotNull(),
                () -> assertThat(responseDto.isValid()).isTrue(),
                () -> assertThat(responseDto.getUserId()).isNotNull().isEqualTo(TestConstant.ID),
                () -> assertThat(responseDto.getRole()).isNotBlank().isEqualTo(TestConstant.ROLE_USER.name()),
                () -> assertThat(responseDto.getErrorMessage()).isNull()
        );
    }

    @Test
    void validateExpiredTokenTest() {
        TokenRequestDto requestDto = TokenRequestDto.builder()
                .token(tokenGenerator.generateAccessToken())
                .build();

        JwtProvider spyJwtProvider = Mockito.spy(jwtProvider);

        when(spyJwtProvider.validateToken(requestDto.getToken()))
                .thenThrow(new ExpiredJwtException(null, null, "Expired"));

        authService = new AuthServiceImpl(credentialsRepository, userServiceClient,
                new BCryptPasswordEncoder(), spyJwtProvider);

        TokenResponseDto responseDto = authService.validateToken(requestDto);

        assertAll(
                () -> assertThat(responseDto).isNotNull(),
                () -> assertThat(responseDto.isValid()).isFalse(),
                () -> assertThat(responseDto.getUserId()).isNotNull().isEqualTo(TestConstant.ID),
                () -> assertThat(responseDto.getRole()).isNotBlank().isEqualTo(TestConstant.ROLE_USER.name()),
                () -> assertThat(responseDto.getErrorMessage()).isNotBlank().contains("Token is expired")
        );
    }

    @Test
    void validateIncorrectTokenTest() {
        TokenRequestDto requestDto = TokenRequestDto.builder()
                .token(tokenGenerator.generateAccessToken().toUpperCase())
                .build();

        TokenResponseDto responseDto = authService.validateToken(requestDto);

        assertAll(
                () -> assertThat(responseDto).isNotNull(),
                () -> assertThat(responseDto.isValid()).isFalse(),
                () -> assertThat(responseDto.getUserId()).isNull(),
                () -> assertThat(responseDto.getRole()).isNull(),
                () -> assertThat(responseDto.getErrorMessage()).isNotBlank().contains("Invalid token")
        );
    }


    @Test
    void refreshTokenSuccessfulTest() {
        TokenRequestDto requestDto = TokenRequestDto.builder()
                .token(tokenGenerator.generateRefreshToken())
                .build();
        UserCredentials userCredentials = DtoBuilder.buildUserCredentials();

        when(credentialsRepository.findCredentialsByUserId(TestConstant.ID))
                .thenReturn(Optional.of(userCredentials));

        AuthResponseDto responseDto = authService.refreshToken(requestDto);

        assertGeneratedTokens(responseDto);

        verify(credentialsRepository, times(1)).findCredentialsByUserId(TestConstant.ID);
    }

    @Test
    void refreshTokenWhenTokenIsNotValidTest() {
        TokenRequestDto requestDto = TokenRequestDto.builder()
                .token(tokenGenerator.generateRefreshToken().toUpperCase())
                .build();

        assertThatThrownBy(() -> authService.refreshToken(requestDto))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refreshTokenWhenTokenIsNotRefreshTest() {
        TokenRequestDto requestDto = TokenRequestDto.builder()
                .token(tokenGenerator.generateAccessToken())
                .build();

        assertThatThrownBy(() -> authService.refreshToken(requestDto))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Not a refresh token");
    }

    @Test
    void refreshTokenWhenUserNotFoundTest() {
        TokenRequestDto requestDto = TokenRequestDto.builder()
                .token(tokenGenerator.generateRefreshToken())
                .build();

        when(credentialsRepository.findCredentialsByUserId(TestConstant.ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(requestDto))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("User not found")
                .hasMessageContaining(String.valueOf(TestConstant.ID));

        verify(credentialsRepository, times(1)).findCredentialsByUserId(TestConstant.ID);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(credentialsRepository, userServiceClient);
    }

    private void assertGeneratedTokens(AuthResponseDto authResponse) {
        assertAll(
                () -> assertThat(jwtProvider.validateToken(authResponse.getAccessToken())).isTrue(),
                () -> assertThat(jwtProvider.extractUserId(authResponse.getAccessToken()))
                        .isNotNull().isEqualTo(TestConstant.ID),
                () -> assertThat(jwtProvider.extractRole(authResponse.getAccessToken()))
                        .isNotNull().isEqualTo(TestConstant.ROLE_USER.name()),
                () -> assertThat(jwtProvider.validateToken(authResponse.getRefreshToken())).isTrue(),
                () -> assertThat(jwtProvider.extractUserId(authResponse.getRefreshToken()))
                        .isNotNull().isEqualTo(TestConstant.ID)
        );
    }
}
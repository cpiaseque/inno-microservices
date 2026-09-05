package com.innowise.userservice.controller;

import com.innowise.userservice.model.dto.UserRequestDto;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.util.TestConstant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends BaseControllerTest {
    private static final String URL = "/users";

    private User testUser;
    private String testToken;

    @Test
    void getUserByIdWhenUserExistsIntegrationTest() throws Exception {
        testUser = userRepository.save(buildTestUser());
        cardRepository.save(buildTestCard(testUser));
        testToken = jwtProvider.generateAccessToken(null, testUser.getId(), TestConstant.ROLE_USER);

        mockMvc.perform(get(String.format("%s/%d", URL, testUser.getId()))
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_USER_NAME).value(testUser.getName()),
                        jsonPath(TestConstant.JSON_PATH_USER_SURNAME).value(testUser.getSurname()),
                        jsonPath(TestConstant.JSON_PATH_USER_BIRTH_DATE)
                                .value(testUser.getBirthDate().format(DateTimeFormatter.ISO_LOCAL_DATE)),
                        jsonPath(TestConstant.JSON_PATH_USER_EMAIL).value(testUser.getEmail()),
                        jsonPath(TestConstant.JSON_PATH_USER_CARDS).isArray(),
                        jsonPath(TestConstant.JSON_PATH_USER_CARDS, hasSize(1))
                );
    }

    @Test
    void getUserByIdWhenUserDoesNotExistIntegrationTest() throws Exception {
        testToken = jwtProvider.generateAccessToken(null, TestConstant.ID, TestConstant.ROLE_ADMIN);

        mockMvc.perform(get(String.format("%s/%d", URL, TestConstant.ID))
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.NOT_FOUND.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value(containsString("User not found with id")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void getUserByIdWithInvalidTokenIntegrationTest() throws Exception {
        mockMvc.perform(get(String.format("%s/%d", URL, TestConstant.ID))
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted("invalid1234")))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.UNAUTHORIZED.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value(containsString("Invalid authentication token")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void getUserByEmailWhenUserExistsIntegrationTest() throws Exception {
        testUser = userRepository.save(buildTestUser());
        testToken = jwtProvider.generateAccessToken(null, TestConstant.ID, TestConstant.ROLE_ADMIN);

        mockMvc.perform(get(URL)
                        .param("email", testUser.getEmail())
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_USER_NAME).value(testUser.getName()),
                        jsonPath(TestConstant.JSON_PATH_USER_SURNAME).value(testUser.getSurname()),
                        jsonPath(TestConstant.JSON_PATH_USER_BIRTH_DATE)
                                .value(testUser.getBirthDate().format(DateTimeFormatter.ISO_LOCAL_DATE)),
                        jsonPath(TestConstant.JSON_PATH_USER_EMAIL).value(testUser.getEmail()),
                        jsonPath(TestConstant.JSON_PATH_USER_CARDS).isArray(),
                        jsonPath(TestConstant.JSON_PATH_USER_CARDS).isEmpty()
                );
    }

    @Test
    void getUserByEmailWhenUserDoesNotExistIntegrationTest() throws Exception {
        testToken = jwtProvider.generateAccessToken(null, TestConstant.ID, TestConstant.ROLE_ADMIN);

        mockMvc.perform(get(URL)
                        .param("email", TestConstant.USER_EMAIL)
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.NOT_FOUND.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value(containsString("User not found with email")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void getUserByEmailWhenParameterIsNotValidIntegrationTest() throws Exception {
        testToken = jwtProvider.generateAccessToken(null, TestConstant.ID, TestConstant.ROLE_ADMIN);

        mockMvc.perform(get(URL)
                        .param("email", "")
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.BAD_REQUEST.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value(containsString("Constraint violation")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_DETAILS, hasItem(containsString("email: must not be blank"))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void getUsersByIdsWhenUserExistsIntegrationTest() throws Exception {
        testUser = userRepository.save(buildTestUser());
        String ids = String.format("%d,%d", testUser.getId(), testUser.getId() + 1);
        testToken = jwtProvider.generateAccessToken(null, TestConstant.ID, TestConstant.ROLE_ADMIN);

        mockMvc.perform(get(URL)
                        .param("ids", ids)
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY).isArray(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY, hasSize(1))
                );
    }

    @Test
    void getUsersByIdsWhenUserDoesNotExistIntegrationTest() throws Exception {
        testToken = jwtProvider.generateAccessToken(null, TestConstant.ID, TestConstant.ROLE_ADMIN);

        mockMvc.perform(get(URL)
                        .param("ids", String.valueOf(TestConstant.ID))
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY).isArray(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY).isEmpty()
                );
    }

    @Test
    void getAllUsersWhenUserExistsIntegrationTest() throws Exception {
        testUser = userRepository.save(buildTestUser());
        testToken = jwtProvider.generateAccessToken(null, TestConstant.ID, TestConstant.ROLE_ADMIN);

        mockMvc.perform(get(URL)
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY).isArray(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY, hasSize(1))
                );
    }

    @Test
    void getAllUsersWhenUserDoesNotExistIntegrationTest() throws Exception {
        testToken = jwtProvider.generateAccessToken(null, TestConstant.ID, TestConstant.ROLE_ADMIN);

        mockMvc.perform(get(URL)
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY).isArray(),
                        jsonPath(TestConstant.JSON_PATH_COMMON_ARRAY).isEmpty()
                );
    }

    @Test
    void getAllUsersWhenAccessDeniedByAuthorityIntegrationTest() throws Exception {
        testToken = jwtProvider.generateAccessToken(null, TestConstant.ID, TestConstant.ROLE_USER);

        mockMvc.perform(get(URL)
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isForbidden(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.FORBIDDEN.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value(containsString("Access Denied")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void createUserSuccessfulIntegrationTest() throws Exception {
        UserRequestDto requestDto = UserRequestDto.builder()
                .name(TestConstant.USER_NAME)
                .surname(TestConstant.USER_NAME)
                .birthDate(TestConstant.LOCAL_DATE_YESTERDAY)
                .email(TestConstant.USER_EMAIL)
                .build();
        testToken = jwtProvider.generateAccessToken(null, null, TestConstant.ROLE_SERVICE);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath(TestConstant.JSON_PATH_USER_NAME).value(requestDto.getName()),
                        jsonPath(TestConstant.JSON_PATH_USER_SURNAME).value(requestDto.getSurname()),
                        jsonPath(TestConstant.JSON_PATH_USER_BIRTH_DATE)
                                .value(requestDto.getBirthDate().format(DateTimeFormatter.ISO_LOCAL_DATE)),
                        jsonPath(TestConstant.JSON_PATH_USER_EMAIL).value(requestDto.getEmail())
                );
    }

    @Test
    void createUserWithNotValidFieldsIntegrationTest() throws Exception {
        UserRequestDto requestDto = UserRequestDto.builder()
                .name(String.join("", TestConstant.USER_NAME, "123"))
                .birthDate(LocalDate.now())
                .email(TestConstant.USER_EMAIL.replace("@", ""))
                .build();
        testToken = jwtProvider.generateAccessToken(null, null, TestConstant.ROLE_SERVICE);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.BAD_REQUEST.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value(containsString("Validation failed")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_DETAILS,
                                hasItem(containsString("Name should start with capital letter and contain only English letters and hyphens"))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_DETAILS, hasItem(containsString("Surname is required"))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_DETAILS, hasItem(containsString("Birth date must be in the past"))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_DETAILS, hasItem(containsString("Email should be valid"))),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void createUserWhenEmailAlreadyExistsIntegrationTest() throws Exception {
        UserRequestDto requestDto = UserRequestDto.builder()
                .name(TestConstant.USER_NAME)
                .surname(TestConstant.USER_NAME)
                .birthDate(TestConstant.LOCAL_DATE_YESTERDAY)
                .email(TestConstant.USER_EMAIL)
                .build();
        testUser = userRepository.save(buildTestUser());
        testToken = jwtProvider.generateAccessToken(null, null, TestConstant.ROLE_SERVICE);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.CONFLICT.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value(containsString("Email already exists in the database")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void updateUserSuccessfulIntegrationTest() throws Exception {
        testUser = userRepository.save(buildTestUser());
        cardRepository.save(buildTestCard(testUser));
        testToken = jwtProvider.generateAccessToken(null, testUser.getId(), TestConstant.ROLE_USER);

        UserRequestDto requestDto = UserRequestDto.builder()
                .name(TestConstant.USER_NAME)
                .surname(TestConstant.NEW_USER_SURNAME)
                .birthDate(TestConstant.NEW_USER_LOCAL_DAY)
                .email(TestConstant.NEW_USER_EMAIL)
                .build();

        doNothing().when(cacheEvictor).evictUser(testUser.getId(), testUser.getEmail(), requestDto.getEmail());

        mockMvc.perform(put((String.format("%s/%d", URL, testUser.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath(TestConstant.JSON_PATH_USER_NAME).value(TestConstant.USER_NAME),
                        jsonPath(TestConstant.JSON_PATH_USER_SURNAME).value(TestConstant.NEW_USER_SURNAME),
                        jsonPath(TestConstant.JSON_PATH_USER_BIRTH_DATE)
                                .value(TestConstant.NEW_USER_LOCAL_DAY.format(DateTimeFormatter.ISO_LOCAL_DATE)),
                        jsonPath(TestConstant.JSON_PATH_USER_EMAIL).value(TestConstant.NEW_USER_EMAIL),
                        jsonPath(TestConstant.JSON_PATH_USER_CARDS).isArray(),
                        jsonPath(TestConstant.JSON_PATH_USER_CARDS, hasSize(1)),
                        jsonPath(String.format("%s[0].holder", TestConstant.JSON_PATH_USER_CARDS))
                                .value(String.join(" ", TestConstant.USER_NAME, TestConstant.NEW_USER_SURNAME).toUpperCase())
                );
    }

    @Test
    void updateUserWhenUserDoesNotExistIntegrationTest() throws Exception {
        UserRequestDto requestDto = UserRequestDto.builder()
                .name(TestConstant.USER_NAME)
                .surname(TestConstant.USER_NAME)
                .birthDate(TestConstant.LOCAL_DATE_YESTERDAY)
                .email(TestConstant.USER_EMAIL)
                .build();
        testToken = jwtProvider.generateAccessToken(null, TestConstant.ID, TestConstant.ROLE_USER);

        mockMvc.perform(put((String.format("%s/%d", URL, TestConstant.ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.NOT_FOUND.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value(containsString("User not found with id")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void updateUserWhenEmailAlreadyExistsIntegrationTest() throws Exception {
        testUser = userRepository.save(buildTestUser());
        testToken = jwtProvider.generateAccessToken(null, testUser.getId(), TestConstant.ROLE_USER);

        User secondUser = buildTestUser();
        secondUser.setEmail(TestConstant.NEW_USER_EMAIL);
        userRepository.save(secondUser);

        UserRequestDto requestDto = UserRequestDto.builder()
                .name(TestConstant.USER_NAME)
                .surname(TestConstant.USER_NAME)
                .birthDate(TestConstant.LOCAL_DATE_YESTERDAY)
                .email(TestConstant.NEW_USER_EMAIL)
                .build();

        mockMvc.perform(put((String.format("%s/%d", URL, testUser.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.CONFLICT.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value(containsString("Email already exists in the database")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }

    @Test
    void deleteUserSuccessfulIntegrationTest() throws Exception {
        testUser = userRepository.save(buildTestUser());
        testToken = jwtProvider.generateAccessToken(null, testUser.getId(), TestConstant.ROLE_USER);

        doNothing().when(cacheEvictor).evictUser(testUser.getId(), testUser.getEmail());

        mockMvc.perform(delete((String.format("%s/%d", URL, testUser.getId())))
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUserWhenUserDoesNotExistIntegrationTest() throws Exception {
        testToken = jwtProvider.generateAccessToken(null, TestConstant.ID, TestConstant.ROLE_ADMIN);

        mockMvc.perform(delete((String.format("%s/%d", URL, TestConstant.ID)))
                        .header(TestConstant.AUTHORIZATION, TestConstant.BEARER_PATTERN.formatted(testToken)))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_STATUS).value(HttpStatus.NOT_FOUND.value()),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_ERROR_MESSAGE).value(containsString("User not found with id")),
                        jsonPath(TestConstant.JSON_PATH_EXCEPTION_TIMESTAMP).exists()
                );
    }
}
package com.innowise.userservice.service;

import com.innowise.userservice.exception.EmailAlreadyExistsException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.model.dto.UserRequestDto;
import com.innowise.userservice.model.dto.UserResponseDto;
import com.innowise.userservice.model.entity.Card;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.service.impl.UserServiceImpl;
import com.innowise.userservice.util.TestConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = UserServiceImpl.class)
class UserServiceTest extends BaseServiceTest {
    @Autowired
    private UserService userService;

    private static User testUser;

    @BeforeAll
    static void beforeAll() {
        testUser = buildTestUser();
    }

    @Test
    void getAllUsersWhenUsersExistTest() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserResponseDto> resultList = userService.getAllUsers();

        assertThat(resultList).isNotNull().isNotEmpty().hasSize(1);

        UserResponseDto resultDto = resultList.get(0);

        assertUserResponseDtoFields(resultDto);

        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getAllUsersWhenUsersDoNotExistTest() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        List<UserResponseDto> resultDto = userService.getAllUsers();

        assertThat(resultDto).isNotNull().isEmpty();

        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUserByIdWhenUserExistsTest() {
        when(userRepository.findUserById(TestConstant.ID)).thenReturn(Optional.of(testUser));

        UserResponseDto resultDto = userService.getUserById(TestConstant.ID);

        assertUserResponseDtoFields(resultDto);

        verify(userRepository, times(1)).findUserById(TestConstant.ID);
    }

    @Test
    void getUserByIdWhenUserDoesNotExistTest() {
        when(userRepository.findUserById(TestConstant.ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(TestConstant.ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found")
                .hasMessageContaining(String.valueOf(TestConstant.ID));

        verify(userRepository, times(1)).findUserById(TestConstant.ID);
    }

    @Test
    void getUsersByIdsWhenUsersExistTest() {
        when(userRepository.findUsersByIdIn(TestConstant.IDS)).thenReturn(List.of(testUser));

        List<UserResponseDto> resultList = userService.getUsersByIds(TestConstant.IDS);

        assertThat(resultList).isNotNull().isNotEmpty().hasSize(1);

        UserResponseDto resultDto = resultList.get(0);

        assertUserResponseDtoFields(resultDto);

        verify(userRepository, times(1)).findUsersByIdIn(TestConstant.IDS);
    }

    @Test
    void getUsersByIdsWhenUsersDoNotExistTest() {
        when(userRepository.findUsersByIdIn(TestConstant.IDS)).thenReturn(Collections.emptyList());

        List<UserResponseDto> resultDto = userService.getUsersByIds(TestConstant.IDS);

        assertThat(resultDto).isNotNull().isEmpty();

        verify(userRepository, times(1)).findUsersByIdIn(TestConstant.IDS);
    }

    @Test
    void getUserByEmailWhenUserExistsTest() {
        when(userRepository.findUserByEmail(TestConstant.USER_EMAIL)).thenReturn(Optional.of(testUser));

        UserResponseDto resultDto = userService.getUserByEmail(TestConstant.USER_EMAIL);

        assertUserResponseDtoFields(resultDto);

        verify(userRepository, times(1)).findUserByEmail(TestConstant.USER_EMAIL);
    }

    @Test
    void getUserByEmailWhenUserDoesNotExistTest() {
        when(userRepository.findUserByEmail(TestConstant.USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail(TestConstant.USER_EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found")
                .hasMessageContaining(TestConstant.USER_EMAIL);

        verify(userRepository, times(1)).findUserByEmail(TestConstant.USER_EMAIL);
    }

    @Test
    void createUserSuccessfulTest() {
        UserRequestDto requestDto = UserRequestDto.builder()
                .email(TestConstant.USER_EMAIL)
                .build();

        when(userRepository.existsByEmail(TestConstant.USER_EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponseDto resultDto = userService.createUser(requestDto);

        assertUserResponseDtoFields(resultDto);

        verify(userRepository, times(1)).existsByEmail(TestConstant.USER_EMAIL);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUserWhenEmailAlreadyExistsTest() {
        UserRequestDto requestDto = UserRequestDto.builder()
                .email(TestConstant.USER_EMAIL)
                .build();

        when(userRepository.existsByEmail(TestConstant.USER_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(requestDto))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email already exists in the database")
                .hasMessageContaining(TestConstant.USER_EMAIL);

        verify(userRepository, times(1)).existsByEmail(TestConstant.USER_EMAIL);
    }

    @Test
    void updateUserWithNewRequestDataTest() {
        User testUser = buildTestUser();
        testUser.setCards(List.of(new Card()));

        UserRequestDto requestDto = UserRequestDto.builder()
                .name(TestConstant.USER_NAME)
                .surname(TestConstant.NEW_USER_SURNAME)
                .birthDate(TestConstant.NEW_USER_LOCAL_DAY)
                .email(TestConstant.NEW_USER_EMAIL)
                .build();

        String expectedHolderName = String.join(" ", TestConstant.USER_NAME, TestConstant.NEW_USER_SURNAME).toUpperCase();

        when(userRepository.findUserById(TestConstant.ID)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(TestConstant.NEW_USER_EMAIL)).thenReturn(false);
        doNothing().when(cardRepository).updateHolderByUserId(TestConstant.ID, expectedHolderName);
        doNothing().when(userRepository).updateUser(TestConstant.ID, TestConstant.USER_NAME,
                TestConstant.NEW_USER_SURNAME, TestConstant.NEW_USER_LOCAL_DAY, TestConstant.NEW_USER_EMAIL);
        doNothing().when(cacheEvictor).evictUser(TestConstant.ID, TestConstant.USER_EMAIL, TestConstant.NEW_USER_EMAIL);

        UserResponseDto resultDto = userService.updateUser(TestConstant.ID, requestDto);

        assertAll(
                () -> assertThat(resultDto).isNotNull(),
                () -> assertThat(resultDto.getName()).isNotBlank().isEqualTo(TestConstant.USER_NAME),
                () -> assertThat(resultDto.getSurname()).isNotBlank().isEqualTo(TestConstant.NEW_USER_SURNAME),
                () -> assertThat(resultDto.getBirthDate()).isNotNull().isEqualTo(TestConstant.NEW_USER_LOCAL_DAY),
                () -> assertThat(resultDto.getEmail()).isNotBlank().isEqualTo(TestConstant.NEW_USER_EMAIL),
                () -> assertThat(resultDto.getCards()).isNotNull().isNotEmpty().hasSize(1),
                () -> assertThat(resultDto.getCards().get(0).getHolder()).isNotBlank().isEqualTo(expectedHolderName)
        );

        verify(userRepository, times(1)).findUserById(TestConstant.ID);
        verify(userRepository, times(1)).existsByEmail(TestConstant.NEW_USER_EMAIL);
        verify(cardRepository, times(1)).updateHolderByUserId(TestConstant.ID, expectedHolderName);
        verify(userRepository, times(1)).updateUser(TestConstant.ID, TestConstant.USER_NAME, TestConstant.NEW_USER_SURNAME, TestConstant.NEW_USER_LOCAL_DAY, TestConstant.NEW_USER_EMAIL);
        verify(cacheEvictor, times(1)).evictUser(TestConstant.ID, TestConstant.USER_EMAIL, TestConstant.NEW_USER_EMAIL);
    }

    @Test
    void updateUserWhenUserDoesNotExistTest() {
        UserRequestDto requestDto = UserRequestDto.builder().build();

        when(userRepository.findUserById(TestConstant.ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(TestConstant.ID, requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found")
                .hasMessageContaining(String.valueOf(TestConstant.ID));

        verify(userRepository, times(1)).findUserById(TestConstant.ID);
    }

    @Test
    void updateUserWhenEmailAlreadyExistsTest() {
        UserRequestDto requestDto = UserRequestDto.builder()
                .email(TestConstant.NEW_USER_EMAIL)
                .build();

        when(userRepository.findUserById(TestConstant.ID)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(TestConstant.NEW_USER_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(TestConstant.ID, requestDto))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email already exists in the database")
                .hasMessageContaining(TestConstant.NEW_USER_EMAIL);

        verify(userRepository, times(1)).findUserById(TestConstant.ID);
        verify(userRepository, times(1)).existsByEmail(TestConstant.NEW_USER_EMAIL);
    }

    @Test
    void deleteUserSuccessfulTest() {
        when(userRepository.findUserById(TestConstant.ID)).thenReturn(Optional.of(testUser));
        doNothing().when(cacheEvictor).evictUser(TestConstant.ID, TestConstant.USER_EMAIL);

        userService.deleteUser(TestConstant.ID);

        verify(userRepository, times(1)).findUserById(TestConstant.ID);
        verify(cacheEvictor, times(1)).evictUser(TestConstant.ID, TestConstant.USER_EMAIL);
        verify(userRepository, times(1)).deleteUserById(TestConstant.ID);
    }

    @Test
    void deleteUserWhenUserDoesNotExistTest() {
        when(userRepository.findUserById(TestConstant.ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(TestConstant.ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found")
                .hasMessageContaining(String.valueOf(TestConstant.ID));

        verify(userRepository, times(1)).findUserById(TestConstant.ID);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(userRepository, cardRepository, cacheEvictor);
    }

    private void assertUserResponseDtoFields(UserResponseDto responseDto) {
        assertAll(
                () -> assertThat(responseDto).isNotNull(),
                () -> assertThat(responseDto.getName()).isNotBlank().isEqualTo(TestConstant.USER_NAME),
                () -> assertThat(responseDto.getSurname()).isNotBlank().isEqualTo(TestConstant.USER_NAME),
                () -> assertThat(responseDto.getBirthDate()).isNotNull().isEqualTo(TestConstant.LOCAL_DATE_YESTERDAY),
                () -> assertThat(responseDto.getEmail()).isNotBlank().isEqualTo(TestConstant.USER_EMAIL)
        );
    }
}
package com.innowise.userservice.service.impl;

import com.innowise.userservice.exception.EmailAlreadyExistsException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.model.dto.UserRequestDto;
import com.innowise.userservice.model.dto.UserResponseDto;
import com.innowise.userservice.model.dto.mapper.UserMapper;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.repository.CardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.UserService;
import com.innowise.userservice.service.cache.CacheEvictor;
import com.innowise.userservice.util.CardFieldsGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final UserMapper userMapper;
    private final CacheEvictor cacheEvictor;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        List<User> retrievedUsers = userRepository.findAll();

        return userMapper.toResponseDtoList(retrievedUsers);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#userId")
    public UserResponseDto getUserById(Long userId) {
        User retrievedUser = userRepository.findUserById(userId)
                .orElseThrow(() -> ResourceNotFoundException.userNotFound(userId));

        return userMapper.toResponseDto(retrievedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getUsersByIds(List<Long> usersIds) {
        List<User> retrievedUsers = userRepository.findUsersByIdIn(usersIds);

        return userMapper.toResponseDtoList(retrievedUsers);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#email")
    public UserResponseDto getUserByEmail(String email) {
        User retrievedUser = userRepository.findUserByEmail(email)
                .orElseThrow(() -> ResourceNotFoundException.userNotFound(email));

        return userMapper.toResponseDto(retrievedUser);
    }

    @Override
    @Transactional
    public UserResponseDto createUser(UserRequestDto userRequestDto) {
        if (userRepository.existsByEmail(userRequestDto.getEmail())) {
            throw new EmailAlreadyExistsException(userRequestDto.getEmail());
        }

        User createdUser = userRepository.save(userMapper.toEntity(userRequestDto));

        return userMapper.toResponseDto(createdUser);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Long userId, UserRequestDto userRequestDto) {
        User updatingUser = userRepository.findUserById(userId)
                .orElseThrow(() -> ResourceNotFoundException.userNotFound(userId));

        if (!updatingUser.getEmail().equals(userRequestDto.getEmail())
                && userRepository.existsByEmail(userRequestDto.getEmail())) {
            throw new EmailAlreadyExistsException(userRequestDto.getEmail());
        }

        boolean nameChanged = !updatingUser.getName().equals(userRequestDto.getName())
                || !updatingUser.getSurname().equals(userRequestDto.getSurname());
        String oldEmail = updatingUser.getEmail();

        userMapper.updateUserFromDto(userRequestDto, updatingUser);

        if (nameChanged && updatingUser.getCards() != null) {
            String updatingHolder = CardFieldsGenerator.formatCardHolderName(updatingUser);
            updatingUser.getCards().forEach(card -> card.setHolder(updatingHolder));

            cardRepository.updateHolderByUserId(updatingUser.getId(), updatingHolder);
        }

        userRepository.updateUser(updatingUser.getId(), updatingUser.getName(), updatingUser.getSurname(),
                updatingUser.getBirthDate(), updatingUser.getEmail());

        cacheEvictor.evictUser(userId, oldEmail, userRequestDto.getEmail());

        return userMapper.toResponseDto(updatingUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User userToDelete = userRepository.findUserById(userId)
                .orElseThrow(() -> ResourceNotFoundException.userNotFound(userId));

        cacheEvictor.evictUser(userId, userToDelete.getEmail());

        userRepository.deleteUserById(userId);
    }
}
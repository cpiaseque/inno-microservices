package com.innowise.userservice.model.dto.mapper;

import com.innowise.userservice.model.dto.UserRequestDto;
import com.innowise.userservice.model.dto.UserResponseDto;
import com.innowise.userservice.model.entity.User;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring",
        uses = CardMapper.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    User toEntity(UserRequestDto userDto);

    @Mapping(source = "id", target = "userId")
    UserResponseDto toResponseDto(User user);

    @Mapping(source = "id", target = "userId")
    List<UserResponseDto> toResponseDtoList(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cards", ignore = true)
    void updateUserFromDto(UserRequestDto userRequestDto, @MappingTarget User user);

    @AfterMapping
    default void handleNullCards(@MappingTarget UserResponseDto userResponseDto) {
        if (userResponseDto.getCards() == null) {
            userResponseDto.setCards(Collections.emptyList());
        }
    }
}
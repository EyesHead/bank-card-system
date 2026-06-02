package com.example.bankcards.util.mapper;

import com.example.bankcards.dto.user.UserRegisterRequestDto;
import com.example.bankcards.dto.user.UserResponseDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {

    public User toEntity(UserRegisterRequestDto dto) {
        return User.builder()
                .username(dto.username())
                .build();
    }

    public UserResponseDto toDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getUsername(),
                user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
        );
    }
}

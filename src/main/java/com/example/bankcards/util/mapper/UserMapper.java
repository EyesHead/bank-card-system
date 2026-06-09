package com.example.bankcards.util.mapper;

import com.example.bankcards.dto.user.UserRegisterRequestDto;
import com.example.bankcards.dto.user.UserResponseDto;
import com.example.bankcards.entity.User;

public interface UserMapper {
    User toEntity(UserRegisterRequestDto dto);
    UserResponseDto toDto(User user);
}

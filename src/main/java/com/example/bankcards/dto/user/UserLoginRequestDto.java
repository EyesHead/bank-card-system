package com.example.bankcards.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UserLoginRequestDto(
    @NotBlank(message = "Username cannot be empty")
    String username,

    @NotBlank(message = "Password cannot be empty")
    String password
){}

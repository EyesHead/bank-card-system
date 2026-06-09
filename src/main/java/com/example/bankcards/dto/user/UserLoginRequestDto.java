package com.example.bankcards.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


@Schema(description = "Запрос на регистрацию пользователя")
public record UserLoginRequestDto(

        @Schema(
                description = "Имя пользователя",
                example = "Daniel"
        )
        @NotBlank(message = "Username cannot be empty")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Schema(
                description = "Пароль",
                example = "StrongPasswordQWE!"
        )
        @NotBlank(message = "Password cannot be empty")
        String password

) {}

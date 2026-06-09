package com.example.bankcards.dto.jwt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на получение JWT auth токена")
public record RefreshTokenRequestDto(
        @Schema(
                description = "Valid refresh token",
                example = "6f7e90e7-cd8b-4dfd-a6a3-34f6c95dcb17"
        )
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}

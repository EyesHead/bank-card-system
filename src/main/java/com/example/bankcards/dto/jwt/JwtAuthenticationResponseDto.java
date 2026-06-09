package com.example.bankcards.dto.jwt;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Ответ после успешной аутентификации")
public record JwtAuthenticationResponseDto(

        @Schema(
                description = "JWT access token. Используется для доступа к защищенным ресурсам",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huIn0.signature"
        )
        String accessToken,

        @Schema(
                description = "Refresh token. Используется для обновления access token",
                example = "6f7e90e7-cd8b-4dfd-a6a3-34f6c95dcb17"
        )
        String refreshToken,

        @Schema(
                description = "Время истечения access token (UTC)",
                example = "2026-06-04T12:30:00Z"
        )
        Instant accessExpiresAt,

        @Schema(
                description = "Время истечения refresh token (UTC)",
                example = "2026-06-11T12:00:00Z"
        )
        Instant refreshExpiresAt

) {}
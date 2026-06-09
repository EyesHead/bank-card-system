package com.example.bankcards.dto.user;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;

@Schema(description = "Информация о пользователе")
public record UserResponseDto(
        @Schema(
                description = "ID пользователя",
                example = "1"
        )
        Long id,

        @Schema(
                description = "Имя пользователя",
                example = "Daniel"
        )
        String username,

        @ArraySchema(
                schema = @Schema(example = "ROLE_USER")
        )
        Set<String> roles,

        @Schema(description = "Время создания (UTC)",
                example = "2026-06-04T12:00:00Z")
        Instant createdAt,


        @Schema(description = "Время последнего обновления (UTC)",
                example = "2026-06-04T12:00:00Z")
        Instant updatedAt
) {}
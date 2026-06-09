package com.example.bankcards.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Сообщение об ошибке API")
public record ErrorResponseDto(
        @Schema(
                description = "Дата и время возникновения ошибки",
                example = "2026-06-04T12:00:00"
        )
        LocalDateTime timeStamp,

        @Schema(
                description = "HTTP статус код",
                example = "404"
        )
        Integer status,

        @Schema(
                description = "Название HTTP ошибки",
                example = "Not Found"
        )
        String error,

        @Schema(
                description = "Описание ошибки",
                example = "User not found"
        )
        String message,

        @Schema(
                description = "Путь запроса",
                example = "/api/auth/login"
        )
        String path
) {}

package com.example.bankcards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Запрос на создание карты")
public record CardCreateRequestDto (
        @NotNull(message = "Owner username is required")
        @Schema(
                description = "Username владельца карты",
                example = "Daniel"
        )
        String username,

        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future")
        @Schema(
                description = "Дата окончания срока действия карты",
                example = "2030-12-31"
        )
        LocalDate expiryDate
){ }

package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Информация о банковской карте")
public record CardResponseDto(
        @Schema(description = "ID карты", example = "1")
        Long id,

        @Schema(description = "Номер карты (маскированный или полный в зависимости от контекста)", example = "**** **** **** 1234")
        String cardNumber,

        @Schema(description = "ID владельца", example = "1")
        Long ownerId,

        @Schema(description = "Имя владельца", example = "Daniel")
        String ownerUsername,

        @Schema(description = "Срок действия", example = "2030-12-31")
        LocalDate expiryDate,

        @Schema(description = "Статус", example = "ACTIVE")
        CardStatus status,

        @Schema(description = "Баланс", example = "15000.50")
        BigDecimal balance,

        @Schema(description = "Дата создания", example = "2026-06-04T12:00:00Z")
        Instant createdAt,

        @Schema(description = "Дата обновления", example = "2026-06-04T12:30:00Z")
        Instant updatedAt
) {}
package com.example.bankcards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Запрос на перевод средств между личными картами")
public record CardTransferRequestDto(
        @Schema(
                description = "Полный 16-значный номер карты отправителя (счёт списания)",
                example = "4000001234567890",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Source card cardNumber is required")
        @Size(min = 16, max = 16, message = "Card cardNumber must be exactly 16 digits")
        String fromCardNumber,

        @Schema(
                description = "Полный 16-значный номер карты получателя (счёт зачисления)",
                example = "4000009876543210",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Target card cardNumber is required")
        @Size(min = 16, max = 16, message = "Card cardNumber must be exactly 16 digits")
        String toCardNumber,

        @Schema(
                description = "Сумма перевода (минимальное значение — 1.00)",
                example = "1000.50",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "1.00",
                message = "Minimum transfer amount is 1.00"
        )
        BigDecimal amount
) {}
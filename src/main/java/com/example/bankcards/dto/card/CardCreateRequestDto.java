package com.example.bankcards.dto.card;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CardCreateRequestDto (
        @NotNull(message = "Owner ID is required")
        Long ownerId,

        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future")
        Instant expiryDate
){
}

package com.example.bankcards.dto.user;

import java.time.Instant;
import java.util.Set;

public record UserResponseDto(
        Long id,
        Instant createdAt,
        Instant updatedAt,
        String username,
        Set<String> roles
) {}

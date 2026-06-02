package com.example.bankcards.dto.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class JwtAuthenticationResponseDto {
    private String accessToken;
    private String refreshToken;
    private Instant accessExpiresAt;
    private Instant refreshExpiresAt;
}
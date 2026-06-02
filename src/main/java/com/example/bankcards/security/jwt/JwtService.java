package com.example.bankcards.security.jwt;

import com.example.bankcards.dto.jwt.JwtAuthenticationResponseDto;

public interface JwtService {
    JwtAuthenticationResponseDto generateTokenPair(
            String username
    );

    String extractUsername(String token);

    JwtAuthenticationResponseDto refreshAccessToken(
            String username,
            String refreshToken
    );

    boolean validateToken(String token);
}

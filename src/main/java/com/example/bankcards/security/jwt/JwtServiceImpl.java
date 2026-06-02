package com.example.bankcards.security.jwt;

import com.example.bankcards.dto.jwt.JwtAuthenticationResponseDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static java.time.Instant.now;

@Component
@Slf4j
public class JwtServiceImpl implements JwtService {

    private final SecretKey secretKey;      // инициализируем один раз в конструкторе
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtServiceImpl(
            @Value("${application.security.jwt.secret-key}") String secret,
            @Value("${application.security.jwt.access-expiration}") long accessExpiration,
            @Value("${application.security.jwt.refresh-expiration}") long refreshExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)); // один раз
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    // ───── Публичное API ─────

    @Override
    public JwtAuthenticationResponseDto generateTokenPair(
            String username) {
        return buildResponse(
                buildAccessToken(username, accessExpiration),
                buildRefreshToken(username, refreshExpiration)
        );
    }

    @Override
    public JwtAuthenticationResponseDto refreshAccessToken(
            String username,
            String refreshToken) {
        return buildResponse(
                buildAccessToken(username, accessExpiration),
                refreshToken
        );
    }

    @Override
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (MalformedJwtException | UnsupportedJwtException | SecurityException e) {
            log.warn("JWT invalid: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT unexpected error: {}", e.getMessage());
        }
        return false;
    }


    private String buildAccessToken(String username, long expiration) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("type", "ACCESS")
                .issuedAt(Date.from(now()))
                .expiration(Date.from(now().plusMillis(expiration)))
                .signWith(secretKey)
                .compact();
    }

    private String buildRefreshToken(String username, long expiration) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("type", "REFRESH")
                .issuedAt(Date.from(now()))
                .expiration(Date.from(now().plusMillis(expiration)))
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private JwtAuthenticationResponseDto buildResponse(String accessToken, String refreshToken) {
        return JwtAuthenticationResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpiresAt(now().plusMillis(accessExpiration))
                .refreshExpiresAt(now().plusMillis(refreshExpiration))
                .build();
    }
}
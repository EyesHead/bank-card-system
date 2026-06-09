package com.example.bankcards.security.jwt;

import com.example.bankcards.dto.jwt.JwtAuthenticationResponseDto;
import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.RefreshTokenRepository;
import com.example.bankcards.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.time.Instant.now;

@Component
@Slf4j
public class JwtServiceImpl implements JwtService {

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public JwtServiceImpl(
            @Value("${application.security.jwt.secret-key}") String secret,
            @Value("${application.security.jwt.access-expiration}") long accessExpiration,
            @Value("${application.security.jwt.refresh-expiration}") long refreshExpiration,
            @Autowired RefreshTokenRepository refreshTokenRepository,
            @Autowired UserRepository userRepository
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public JwtAuthenticationResponseDto generateTokenPair(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        String accessToken = buildAccessToken(username, roles, accessExpiration);
        String refreshTokenString = buildRefreshToken(username, refreshExpiration);

        RefreshToken refreshTokenEntity = refreshTokenRepository.findByUser(user)
                .orElse(RefreshToken.builder()
                        .user(user)
                        .build());

        refreshTokenEntity.setToken(refreshTokenString);
        refreshTokenEntity.setExpiryDate(now().plusMillis(refreshExpiration));

        refreshTokenRepository.save(refreshTokenEntity);

        return buildResponse(accessToken, refreshTokenString);
    }

    @Override
    @Transactional
    public JwtAuthenticationResponseDto refreshAccessToken(String username, String refreshToken) {
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new JwtException("Refresh token is missing in database or compromised"));

        if (refreshTokenEntity.getExpiryDate().isBefore(now())) {
            refreshTokenRepository.delete(refreshTokenEntity);
            throw new JwtException("Refresh token was expired. Please sign in again");
        }

        User user = refreshTokenEntity.getUser();
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        String newAccessToken = buildAccessToken(user.getUsername(), roles, accessExpiration);
        String newRefreshToken = buildRefreshToken(user.getUsername(), refreshExpiration);

        refreshTokenEntity.setToken(newRefreshToken);
        refreshTokenEntity.setExpiryDate(now().plusMillis(refreshExpiration));
        refreshTokenRepository.save(refreshTokenEntity);

        return buildResponse(newAccessToken, newRefreshToken);    }

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


    private String buildAccessToken(String username, List<String> roles, long expiration) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("type", "ACCESS")
                .claim("roles", roles)
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
        return new JwtAuthenticationResponseDto(
                accessToken,
                refreshToken,
                now().plusMillis(accessExpiration),
                now().plusMillis(refreshExpiration)
                );
    }
}
package com.example.bankcards.controller;

import com.example.bankcards.dto.jwt.JwtAuthenticationResponseDto;
import com.example.bankcards.dto.jwt.RefreshTokenRequestDto;
import com.example.bankcards.dto.user.UserLoginRequestDto;
import com.example.bankcards.dto.user.UserRegisterRequestDto;
import com.example.bankcards.dto.user.UserResponseDto;
import com.example.bankcards.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Test
    @DisplayName("Register user successfully")
    void registerUser_success() throws Exception {

        UserRegisterRequestDto request =
                new UserRegisterRequestDto(
                        "daniel",
                        "password123"
                );

        UserResponseDto response =
                new UserResponseDto(
                        1L,
                        Instant.now(),
                        Instant.now(),
                        "daniel",
                        Set.of("ROLE_USER")
                );

        when(authService.register(any(UserRegisterRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("daniel"));

        verify(authService).register(any(UserRegisterRequestDto.class));
    }

    @Test
    @DisplayName("Register user validation failed")
    void registerUser_validationError() throws Exception {

        UserRegisterRequestDto request =
                new UserRegisterRequestDto(
                        "",
                        "123"
                );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Login successfully")
    void login_success() throws Exception {

        UserLoginRequestDto request =
                new UserLoginRequestDto(
                        "daniel",
                        "password123"
                );

        JwtAuthenticationResponseDto response =
                JwtAuthenticationResponseDto.builder()
                        .accessToken("access-token")
                        .refreshToken("refresh-token")
                        .accessExpiresAt(Instant.now())
                        .refreshExpiresAt(Instant.now())
                        .build();

        when(authenticationManager.authenticate(any()))
                .thenReturn(
                        new UsernamePasswordAuthenticationToken(
                                request.username(),
                                request.password()
                        )
                );

        when(authService.signIn(any(UserLoginRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("access-token"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("refresh-token"));

        verify(authenticationManager).authenticate(any());
        verify(authService).signIn(any(UserLoginRequestDto.class));
    }

    @Test
    @DisplayName("Login validation failed")
    void login_validationError() throws Exception {

        UserLoginRequestDto request =
                new UserLoginRequestDto(
                        "",
                        ""
                );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
        verifyNoInteractions(authenticationManager);
    }

    @Test
    @DisplayName("Refresh token successfully")
    void refresh_success() throws Exception {

        RefreshTokenRequestDto request =
                new RefreshTokenRequestDto(
                        "refresh-token"
                );

        JwtAuthenticationResponseDto response =
                JwtAuthenticationResponseDto.builder()
                        .accessToken("new-access-token")
                        .refreshToken("refresh-token")
                        .accessExpiresAt(Instant.now())
                        .refreshExpiresAt(Instant.now())
                        .build();

        when(authService.refreshToken(any(RefreshTokenRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("new-access-token"));

        verify(authService)
                .refreshToken(any(RefreshTokenRequestDto.class));
    }

    @Test
    @DisplayName("Refresh token validation failed")
    void refresh_validationError() throws Exception {

        RefreshTokenRequestDto request =
                new RefreshTokenRequestDto("");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}
package com.example.bankcards.controller;

import com.example.bankcards.TestDataFactory;
import com.example.bankcards.dto.jwt.JwtAuthenticationResponseDto;
import com.example.bankcards.dto.user.UserLoginRequestDto;
import com.example.bankcards.dto.user.UserRegisterRequestDto;
import com.example.bankcards.dto.user.UserResponseDto;
import com.example.bankcards.exception.UserAlreadyExistsException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Test
    void register_shouldReturn201() throws Exception {
        UserRegisterRequestDto request = new UserRegisterRequestDto("newuser", "password123");
        UserResponseDto response = TestDataFactory.userDto(2L, "newuser", Set.of("ROLE_USER"));

        when(authService.register(any(UserRegisterRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void register_shouldReturn409WhenUserExists() throws Exception {
        UserRegisterRequestDto request = new UserRegisterRequestDto("existing", "password123");

        when(authService.register(any(UserRegisterRequestDto.class)))
                .thenThrow(new UserAlreadyExistsException("Username already taken: existing"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_shouldReturn400WhenInvalidData() throws Exception {
        UserRegisterRequestDto request = new UserRegisterRequestDto("ab", "12");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturn200() throws Exception {
        UserLoginRequestDto request = new UserLoginRequestDto("testuser", "password");
        JwtAuthenticationResponseDto response = new JwtAuthenticationResponseDto(
                "access-token", "refresh-token",
                Instant.now().plusSeconds(900), Instant.now().plusSeconds(604800));

        Authentication authMock = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authMock);
        when(authService.signIn(any(UserLoginRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void login_shouldReturn401WhenBadCredentials() throws Exception {
        UserLoginRequestDto request = new UserLoginRequestDto("testuser", "wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_shouldReturn404WhenUserNotFound() throws Exception {
        UserLoginRequestDto request = new UserLoginRequestDto("unknown", "password");

        Authentication authMock = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authMock);
        when(authService.signIn(any(UserLoginRequestDto.class)))
                .thenThrow(new UserNotFoundException("User not found: unknown"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void refreshToken_shouldReturn200() throws Exception {
        String refreshTokenJson = """
                {
                    "refreshToken": "valid-refresh-token"
                }
                """;
        JwtAuthenticationResponseDto response = new JwtAuthenticationResponseDto(
                "new-access", "new-refresh",
                Instant.now().plusSeconds(900), Instant.now().plusSeconds(604800));

        when(authService.refreshToken(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }
}
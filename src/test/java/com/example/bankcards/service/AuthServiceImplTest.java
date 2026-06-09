package com.example.bankcards.service;

import com.example.bankcards.TestDataFactory;
import com.example.bankcards.dto.jwt.JwtAuthenticationResponseDto;
import com.example.bankcards.dto.jwt.RefreshTokenRequestDto;
import com.example.bankcards.dto.user.UserLoginRequestDto;
import com.example.bankcards.dto.user.UserRegisterRequestDto;
import com.example.bankcards.dto.user.UserResponseDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AuthenticationException;
import com.example.bankcards.exception.UserAlreadyExistsException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.jwt.JwtService;
import com.example.bankcards.service.impl.AuthServiceImpl;
import com.example.bankcards.util.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_shouldCreateUser() {
        UserRegisterRequestDto dto = new UserRegisterRequestDto("newuser", "password123");
        Role userRole = TestDataFactory.roleUser();
        User unsavedUser = new User();
        unsavedUser.setUsername("newuser");

        User savedUser = TestDataFactory.regularUser(3L, "newuser");
        UserResponseDto expectedDto = TestDataFactory.userDto(3L, "newuser", Set.of("ROLE_USER"));

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userMapper.toEntity(dto)).thenReturn(unsavedUser);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(expectedDto);

        UserResponseDto result = authService.register(dto);

        assertNotNull(result);
        assertEquals("newuser", result.username());
        assertEquals(3L, result.id());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowWhenUsernameExists() {
        UserRegisterRequestDto dto = new UserRegisterRequestDto("existing", "password123");
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void signIn_shouldReturnTokenPair() {
        UserLoginRequestDto dto = new UserLoginRequestDto("testuser", "password");
        User user = TestDataFactory.regularUser();
        JwtAuthenticationResponseDto tokenResponse = new JwtAuthenticationResponseDto(
                "access-token", "refresh-token",
                Instant.now(), Instant.now().plusSeconds(3600));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtService.generateTokenPair("testuser")).thenReturn(tokenResponse);

        passwordEncoder.matches(
                dto.password(),
                user.getPassword()
        );

        JwtAuthenticationResponseDto result = authService.signIn(dto);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
    }

    @Test
    void signIn_shouldThrowWhenUserNotFound() {
        UserLoginRequestDto dto = new UserLoginRequestDto("unknown", "password");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.signIn(dto));
    }

    @Test
    void refreshToken_shouldReturnNewToken() {
        RefreshTokenRequestDto dto = new RefreshTokenRequestDto("valid-refresh-token");
        User user = TestDataFactory.regularUser();
        JwtAuthenticationResponseDto tokenResponse = new JwtAuthenticationResponseDto(
                "new-access", "new-refresh",
                Instant.now(), Instant.now().plusSeconds(3600));

        when(jwtService.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-refresh-token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtService.refreshAccessToken("testuser", "valid-refresh-token")).thenReturn(tokenResponse);

        JwtAuthenticationResponseDto result = authService.refreshToken(dto);

        assertNotNull(result);
        assertEquals("new-access", result.accessToken());
    }

    @Test
    void refreshToken_shouldThrowWhenTokenInvalid() {
        RefreshTokenRequestDto dto = new RefreshTokenRequestDto("invalid-token");
        when(jwtService.validateToken("invalid-token")).thenReturn(false);

        assertThrows(AuthenticationException.class, () -> authService.refreshToken(dto));
    }
}
package com.example.bankcards.controller;

import com.example.bankcards.dto.jwt.JwtAuthenticationResponseDto;
import com.example.bankcards.dto.jwt.RefreshTokenRequestDto;
import com.example.bankcards.dto.user.UserLoginRequestDto;
import com.example.bankcards.dto.user.UserRegisterRequestDto;
import com.example.bankcards.dto.user.UserResponseDto;
import com.example.bankcards.exception.UserAlreadyExistsException;
import com.example.bankcards.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> registerUser(@Valid @RequestBody UserRegisterRequestDto userRequestDto) throws UserAlreadyExistsException {
        log.info("New request for registering user: '{}'", userRequestDto.username());

        UserResponseDto result = authService.register(userRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAuthenticationResponseDto> authenticateUser(
            @Valid @RequestBody UserLoginRequestDto userLoginRequestDto) {

        log.info("New request for authenticate user: '{}'", userLoginRequestDto.username());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userLoginRequestDto.username(),
                        userLoginRequestDto.password()
                )
        );

        JwtAuthenticationResponseDto jwtAuthenticationResponseDto = authService.signIn(userLoginRequestDto);
        return ResponseEntity.ok(jwtAuthenticationResponseDto);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthenticationResponseDto> refreshToken(@Valid @RequestBody RefreshTokenRequestDto refreshTokenRequestDto) {
        log.info("New request for refresh token");
        return ResponseEntity.ok(authService.refreshToken(refreshTokenRequestDto));
    }
}

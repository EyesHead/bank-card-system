package com.example.bankcards.service;

import com.example.bankcards.dto.jwt.JwtAuthenticationResponseDto;
import com.example.bankcards.dto.jwt.RefreshTokenRequestDto;
import com.example.bankcards.dto.user.UserLoginRequestDto;
import com.example.bankcards.dto.user.UserRegisterRequestDto;
import com.example.bankcards.dto.user.UserResponseDto;
import com.example.bankcards.exception.UserAlreadyExistsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface AuthService {
    JwtAuthenticationResponseDto signIn(UserLoginRequestDto userCredentialsDto) throws UsernameNotFoundException;
    JwtAuthenticationResponseDto refreshToken(RefreshTokenRequestDto refreshTokenRequestDto);
    UserResponseDto register(UserRegisterRequestDto userRegisterRequestDto) throws UserAlreadyExistsException;
}

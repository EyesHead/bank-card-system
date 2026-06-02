package com.example.bankcards.service.impl;

import com.example.bankcards.dto.jwt.JwtAuthenticationResponseDto;
import com.example.bankcards.dto.jwt.RefreshTokenRequestDto;
import com.example.bankcards.dto.user.UserLoginRequestDto;
import com.example.bankcards.dto.user.UserRegisterRequestDto;
import com.example.bankcards.dto.user.UserResponseDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AuthenticationException;
import com.example.bankcards.exception.UserAlreadyExistsException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.jwt.JwtService;
import com.example.bankcards.service.AuthService;
import com.example.bankcards.util.mapper.UserMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public JwtAuthenticationResponseDto signIn(UserLoginRequestDto dto) throws UsernameNotFoundException {
        String userName = dto.username();
        User user = userRepository.findByUsername(userName)
                .orElseThrow( () -> new UsernameNotFoundException(userName));

        return jwtService.generateTokenPair(user.getUsername());
    }

    @Override
    public JwtAuthenticationResponseDto refreshToken(RefreshTokenRequestDto dto) {

        String refreshToken = dto.refreshToken();

        if (!jwtService.validateToken(refreshToken)) {
            throw new AuthenticationException(
                    "Refresh token is invalid"
            );
        }

        String username = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow( () -> new UsernameNotFoundException(username));

        return jwtService.refreshAccessToken(username, refreshToken);
    }

    @Override
    @Transactional
    public UserResponseDto register(UserRegisterRequestDto dto) throws UserAlreadyExistsException {
        log.info("Registering user '{}'", dto.username());

        if (userRepository.existsByUsername(dto.username())) {
            log.warn("Username '{}' already exists", dto.username());
            throw new UserAlreadyExistsException("Username already taken: " + dto.username());
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "ROLE_USER not found"
                        ));

        User user = userMapper.toEntity(dto);
        user.setRoles(new HashSet<>(Set.of(userRole)));;
        user.setPassword(passwordEncoder.encode(dto.password()));
        User saved = userRepository.save(user);

        UserResponseDto userResponseDto = userMapper.toDto(saved);
        log.info("User '{}' registered successfully", saved.getUsername());

        return userResponseDto;
    }
}

package com.example.bankcards.controller;

import com.example.bankcards.dto.jwt.JwtAuthenticationResponseDto;
import com.example.bankcards.dto.jwt.RefreshTokenRequestDto;
import com.example.bankcards.dto.user.UserLoginRequestDto;
import com.example.bankcards.dto.user.UserRegisterRequestDto;
import com.example.bankcards.dto.user.UserResponseDto;
import com.example.bankcards.exception.ErrorResponseDto;
import com.example.bankcards.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
        name = "Аутентификация",
        description = "(ALL ROLES) Регистрация пользователей, вход в систему и управление сессиями JWT"
)
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = "Некорректный формат тела запроса или синтаксическая ошибка валидации полей DTO (MethodArgumentNotValidException)",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Внутренняя ошибка сервера",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
})
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создает новый аккаунт в системе с уникальным именем. Каждому новому пользователю автоматически присваивается роль ROLE_USER."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Пользователь успешно зарегистрирован, возвращены его данные",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Конфликт данных: пользователь с таким именем уже существует (UserAlreadyExistsException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    public ResponseEntity<UserResponseDto> registerUser(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody
            UserRegisterRequestDto userRequestDto
    ) {
        log.info("Auth: New request for registering user: '{}'", userRequestDto.username());
        UserResponseDto result = authService.register(userRequestDto);
        log.info("Auth: User was registered successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Аутентификация пользователя (Вход)",
            description = "Проверяет логин и пароль. В случае успеха генерирует пару токенов: Access Token (краткосрочный) и Refresh Token (долгосрочный)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешная аутентификация, токены сгенерированы и возвращены",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtAuthenticationResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверное имя пользователя или пароль (BadCredentialsException / AuthenticationException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь с указанным логином не зарегистрирован в системе (UserNotFoundException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    public ResponseEntity<JwtAuthenticationResponseDto> authenticateUser(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody
            UserLoginRequestDto loginRequestDto
    ) {

        log.info("Auth: New request for authenticate user: '{}'", loginRequestDto.username());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDto.username(),
                        loginRequestDto.password()
                )
        );

        JwtAuthenticationResponseDto jwtAuthenticationResponseDto = authService.signIn(loginRequestDto);
        log.info("Auth: User was authenticated: '{}'", loginRequestDto.username());
        return ResponseEntity.ok(jwtAuthenticationResponseDto);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Обновление Access Token по Refresh Token",
            description = "Проверяет валидность и срок действия Refresh токена, после чего выписывает новый Access Token для продолжения безопасной сессии."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Токены успешно перевыпущены (сформирована новая пара или обновлен Access)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtAuthenticationResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh токен недействителен, изменен, просрочен или не прошел верификацию подписи (AuthenticationException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь, указанный в токене, больше не существует в базе данных (UserNotFoundException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    public ResponseEntity<JwtAuthenticationResponseDto> refreshToken(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody
            RefreshTokenRequestDto refreshTokenRequestDto
    ) {
        log.info("Auth: New request for refresh token");
        JwtAuthenticationResponseDto authTokenResponse = authService.refreshToken(refreshTokenRequestDto);
        log.info("Auth: Token successfully refreshed");
        return ResponseEntity.ok(authTokenResponse);
    }
}
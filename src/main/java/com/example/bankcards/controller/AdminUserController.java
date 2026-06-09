package com.example.bankcards.controller;

import com.example.bankcards.dto.user.UserResponseDto;
import com.example.bankcards.exception.ErrorResponseDto;
import com.example.bankcards.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@Validated
@RequiredArgsConstructor
@Tag(
        name = "Администрирование пользователей",
        description = "(ADMIN_ROLE) Управление пользователями системы"
)
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Пользователь не аутентифицирован (отсутствует или невалиден JWT токен)",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class)
                )
        ),
        @ApiResponse(
                responseCode = "403",
                description = "Недостаточно прав доступа. Требуется роль ROLE_ADMIN",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class)
                )
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Внутренняя ошибка сервера",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class)
                )
        )
})
public class AdminUserController {
    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(
            summary = "Получить список пользователей",
            description = "Возвращает пользователей системы в формате PagedModel. " +
                    "Поддерживается пагинация и сортировка."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список пользователей успешно получен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(
                                    implementation = PagedModel.class,
                                    subTypes = {UserResponseDto.class}
                            )
                    )
            )
    })
    public ResponseEntity<PagedModel<UserResponseDto>> getUsers(
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.info("ADMIN: REST request to get all users [page={}, size={}, sort={}]",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        PagedModel<UserResponseDto> users = adminUserService.getAllUsers(pageable);

        log.debug("Successfully fetched {} users for page {}", users.getContent().size(), pageable.getPageNumber());

        return ResponseEntity.ok(users);
    }

    @GetMapping("/{username}")
    @Operation(
            summary = "Получить пользователя по логину",
            description = "Возвращает информацию о пользователе по его уникальному username."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден (UserNotFoundException)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    public ResponseEntity<UserResponseDto> getUserCredentials(
            @PathVariable
            @Pattern(
                    regexp = "^[a-zA-Z0-9_\\-.]{3,50}$",
                    message = "Введен неверный формат username"
            )
            @Parameter(description = "Уникальный username пользователя", example = "Daniel", required = true)
            String username
    ) {

        log.info("ADMIN: REST request to get user '{}'", username);
        UserResponseDto user = adminUserService.getUserByName(username);
        log.info("User '{}' successfully fetched", username);

        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{username}")
    @Operation(
            summary = "Удалить пользователя",
            description = """
                    Физически удаляет пользователя из системы вместе с привязанными к его аккаунту картами.
                    
                    Ограничения:
                    - нельзя удалить администратора;
                    - нельзя удалить пользователя, если хотя бы на одной его карте есть положительный баланс.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Пользователь успешно удален"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден (UserNotFoundException)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = """
                            Операция удаления запрещена:
                            - пользователь является администратором;
                            - на картах пользователя присутствуют денежные средства. (IllegalOperationException)
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    public ResponseEntity<Void> deleteUser(
            @PathVariable
            @Pattern(
                    regexp = "^[a-zA-Z0-9_\\-.]{3,50}$",
                    message = "Введен неверный формат username"
            )
            @Parameter(
                    description = "Уникальный username пользователя",
                    example = "ivan_ivanov",
                    required = true
            )
            String username
    ) {
        log.warn("ADMIN: REST request to DELETE user '{}'", username);

        adminUserService.deleteUserByName(username);

        log.info("User '{}' successfully deleted", username);

        return ResponseEntity.noContent().build();
    }
}

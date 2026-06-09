package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardCreateRequestDto;
import com.example.bankcards.dto.card.CardResponseDto;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.ErrorResponseDto;
import com.example.bankcards.service.CardAdminService;
import com.example.bankcards.util.CardMaskingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/cards")
@Validated
@RequiredArgsConstructor
@Tag(
        name = "Администрирование карт",
        description = "(ADMIN_ROLE) Панель управления банковскими картами системы"
)
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Пользователь не аутентифицирован (отсутствует или невалиден JWT токен)",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
                responseCode = "403",
                description = "Недостаточно прав доступа. Требуется роль ROLE_ADMIN",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Внутренняя ошибка сервера",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
})
public class AdminCardController {
    private final CardMaskingService maskingService;
    private final CardAdminService cardAdminService;

    @PostMapping
    @Operation(
            summary = "Зарегистрировать банковскую карту",
            description = "Генерирует новую карту для указанного пользователя системы. Баланс инициализируется нулем, номер генерируется по алгоритму Луна."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Карта успешно создана",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CardResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса (ошибки валидации DTO)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Указанный владелец карты (username) не найден в системе (UserNotFoundException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    public ResponseEntity<CardResponseDto> createCard(
            @Valid @RequestBody CardCreateRequestDto dto
    ) {
        log.info("ADMIN: REST request to create card for user='{}'", dto.username());
        CardResponseDto response = cardAdminService.createCard(dto);
        log.info("Card successfully created with ID='{}' for user='{}'", response.id(), dto.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Получить список всех карт в системе",
            description = "Возвращает список карт в формате PagedModel. " +
                    "Доступна опциональная фильтрация по логину (username) владельца и текущему статусу."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список карт успешно получен",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PagedModel.class, subTypes = {CardResponseDto.class})
                    )
            )
    })
    public ResponseEntity<PagedModel<CardResponseDto>> getAllCards(
            @RequestParam(name = "username", required = false)
            @Pattern(
                    regexp = "^[a-zA-Z0-9_\\-.]{3,50}$",
                    message = "Введен неверный формат username"
            )
            @Parameter(description = "Логин пользователя для фильтрации (точное совпадение)", example = "ivan_ivanov")
            String username,

            @RequestParam(name = "status", required = false)
            @Parameter(description = "Фильтр по статусу карты", example = "ACTIVE")
            CardStatus status,

            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.info("ADMIN: REST request to get all cards. Filter: " +
                        "[userName={}, status={}], Pageable: [page={}, size={}, sort={}]",
                username, status, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Pageable validatedPageable = pageable;

        boolean hasInvalidSort = pageable.getSort().stream()
                .anyMatch(order -> order.getProperty().equalsIgnoreCase("string"));

        if (hasInvalidSort) {
            log.warn("Invalid sort property 'string' detected. Fallback to default sort by 'createdAt DESC'");
            validatedPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }

        PagedModel<CardResponseDto> cards = cardAdminService.getAllCards(username, status, validatedPageable);

        log.debug("Successfully fetched {} cards for page {}",
                cards.getContent().size(), validatedPageable.getPageNumber());
        return ResponseEntity.ok(cards);
    }

    @PatchMapping("/{cardNumber}/block")
    @Operation(
            summary = "Административная блокировка карты",
            description = "Принудительно переводит карту в статус BLOCKED. Если карта уже заблокирована, метод вернет её текущее состояние без ошибок."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно заблокирована администратором",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта с указанным номером не найдена (CardNotFoundException)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Конфликт бизнес-логики: невозможно заблокировать просроченную карту (CardStatusException)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    public ResponseEntity<CardResponseDto> blockCard(
            @PathVariable
            @Pattern(regexp = "^\\d{16}$", message = "Номер карты должен состоять из 16 цифр. Другие символы - запрещены")
            @Parameter(description = "Номер банковской карты (16 знаков)", required = true, example = "1234567890123456")
            String cardNumber
    ) {
        String maskedCardNumber = maskingService.mask(cardNumber);
        log.info("ADMIN: REST request to block card '{}'", maskedCardNumber);
        CardResponseDto response = cardAdminService.blockCard(cardNumber);
        log.info("Card '{}' successfully blocked by admin", maskedCardNumber);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{cardNumber}/activate")
    @Operation(
            summary = "Активация/Разблокировка карты",
            description = "Переводит карту из любого состояния (например, BLOCKED) обратно в статус ACTIVE."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно активирована",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта с указанным номером не найдена (CardNotFoundException)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    public ResponseEntity<CardResponseDto> activateCard(
            @PathVariable
            @Pattern(regexp = "^\\d{16}$", message = "Номер карты должен состоять из 16 цифр. Другие символы - запрещены")
            @Parameter(description = "Номер банковской карты (16 знаков)", required = true, example = "1234567890123456")
            String cardNumber
    ) {
        String maskedCardNumber = maskingService.mask(cardNumber);
        log.info("ADMIN: REST request to activate card '{}'", maskedCardNumber);
        CardResponseDto response = cardAdminService.activateCard(cardNumber);
        log.info("Card '{}' was successfully activated by admin", maskedCardNumber);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{cardNumber}")
    @Operation(
            summary = "Удалить карту из системы",
            description = "Физически удаляет запись банковской карты из базы данных по её уникальному идентификатору."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Карта успешно удалена, тело ответа отсутствует"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта с указанным номером не найдена (CardNotFoundException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    public ResponseEntity<Void> deleteCard(
            @Pattern(regexp = "^\\d{16}$", message = "Номер карты должен состоять из 16 цифр. Другие символы - запрещены")
            @PathVariable
            @Parameter(description = "Номер банковской карты для безвозвратного удаления (16 знаков)", required = true, example = "1234567890123456")
            String cardNumber
    ) {
        String maskedCardNumber = maskingService.mask(cardNumber);
        log.warn("ADMIN: REST request to DELETE card '{}'", maskedCardNumber);
        cardAdminService.deleteCard(cardNumber);
        log.info("Card '{}' successfully deleted from database", maskedCardNumber);
        return ResponseEntity.noContent().build();
    }
}
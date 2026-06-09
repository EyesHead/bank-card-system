package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardResponseDto;
import com.example.bankcards.dto.card.CardTransferRequestDto;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.ErrorResponseDto;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.CardUserService;
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
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Карты пользователя",
        description = "(USER_ROLE) Просмотр баланса, управление личными картами и переводы между счетами"
)
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Пользователь не аутентифицирован (токен отсутствует, протух или невалиден)",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Внутренняя ошибка сервера",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
        )
})
public class UserCardController {

    private final CardUserService cardUserService;
    private final CardMaskingService maskingService;

    @GetMapping("/cards")
    @Operation(
            summary = "Получить список моих карт",
            description = "Возвращает пагинированный список карт, принадлежащих текущему авторизованному пользователю. Доступна фильтрация по статусу."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список личных карт успешно получен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PagedModel.class, subTypes = {CardResponseDto.class}))
            )
    })
    public ResponseEntity<PagedModel<CardResponseDto>> getMyCards(
            @RequestParam(required = false)
            @Parameter(description = "Фильтр по статусу карты (например, ACTIVE, BLOCKED)", example = "ACTIVE")
            CardStatus status,

            @PageableDefault(size = 20, sort = "createdAt")
            @Parameter(description = "Параметры пагинации (page, size, sort)")
            Pageable pageable,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails userDetails
    ) {
        log.info("User '{}': get own cards [status={}]", userDetails.getUsername(), status);
        return ResponseEntity.ok(cardUserService.getMyCards(userDetails.id(), status, pageable));
    }

    @GetMapping("/cards/{cardNumber}/balance")
    @Operation(
            summary = "Получить баланс карты",
            description = "Возвращает текущий остаток денежных средств на карте. Запрос разрешен только владельцу карты."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Баланс карты успешно получен",
                    content = @Content(mediaType = "text/plain", schema = @Schema(implementation = BigDecimal.class, example = "1500.50"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидный формат номера карты (строка должна содержать строго 16 цифр)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Доступ запрещен: карта принадлежит другому пользователю (CardAccessDeniedException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Указанная карта не найдена в системе (CardNotFoundException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    public ResponseEntity<BigDecimal> getCardBalance(
            @PathVariable
            @Size(min = 16, max = 16, message = "Card cardNumber must be exactly 16 digits")
            @Pattern(regexp = "^\\d{16}$", message = "Номер карты должен состоять из 16 цифр. Другие символы - запрещены")
            @Parameter(description = "Полный 16-значный номер карты", required = true, example = "4000123456789010")
            String cardNumber,

            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails
    ) {
        log.info("User '{}': get balance card={}", userDetails.getUsername(), maskingService.mask(cardNumber));
        return ResponseEntity.ok(cardUserService.getCardBalance(userDetails.id(), cardNumber));
    }

    @PostMapping("/cards/transfer")
    @Operation(
            summary = "Перевод между своими картами",
            description = "Безопасно переводит средства со счета одной вашей карты на другую. Обе карты должны быть активны и принадлежать вам."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Перевод успешно выполнен, балансы счетов обновлены"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Нарушение бизнес-правил: перевод на ту же карту (InvalidTransferException), карта неактивна (CardStatusException) или недостаточно средств (InsufficientFundsException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Попытка использовать чужую карту в качестве источника или получателя (CardAccessDeniedException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Одна или обе карты не найдены в системе (CardNotFoundException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    public ResponseEntity<Void> transferBetweenMyAccount(
            @Valid @RequestBody CardTransferRequestDto dto,
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails
    ) {
        log.info("User '{}': transfer {} from {} to {}",
                userDetails.getUsername(), dto.amount(),
                maskingService.mask(dto.fromCardNumber()), maskingService.mask(dto.toCardNumber()));

        cardUserService.transferBetweenAccount(userDetails.id(), dto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/cards/{cardNumber}/block")
    @Operation(
            summary = "Заблокировать карту",
            description = "Пользовательская блокировка личной карты. Переводит карту из статуса ACTIVE в статус BLOCKED."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно заблокирована, возвращены обновленные данные",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CardResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Карта уже имеет статус, отличный от ACTIVE (CardStatusException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Попытка заблокировать карту, принадлежащую другому пользователю (CardAccessDeniedException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта с таким номером не найдена (CardNotFoundException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    public ResponseEntity<CardResponseDto> requestBlock(
            @PathVariable
            @Size(min = 16, max = 16, message = "Card cardNumber must be exactly 16 digits")
            @Pattern(regexp = "^\\d{16}$", message = "Номер карты должен состоять из 16 цифр. Другие символы - запрещены")
            @Parameter(description = "Полный 16-значный номер блокируемой карты", required = true, example = "4000123456789010")
            String cardNumber,

            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("User '{}': request block card {}", userDetails.getUsername(), maskingService.mask(cardNumber));
        return ResponseEntity.ok(cardUserService.requestBlock(userDetails.id(), cardNumber));
    }
}
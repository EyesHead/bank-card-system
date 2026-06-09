package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardResponseDto;
import com.example.bankcards.dto.card.CardTransferRequestDto;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;

import java.math.BigDecimal;

/**
 * Сервис операций пользователя с собственными банковскими картами.
 *
 * <p>Все методы применяют проверку владения картой — пользователь
 * может работать только со своими картами. Попытка получить доступ
 * к чужой карте выбрасывает {@link CardAccessDeniedException}.
 */
public interface CardUserService {

    /**
     * Возвращает постраничный список карт текущего пользователя.
     *
     * @param userId   идентификатор пользователя
     * @param status   статус карты для фильтрации, {@code null} — без фильтра
     * @param pageable параметры пагинации и сортировки
     * @return страница карт пользователя
     */
    PagedModel<CardResponseDto> getMyCards(Long userId, CardStatus status, Pageable pageable);

    /**
     * Возвращает текущий баланс карты.
     *
     * @param userId     идентификатор пользователя
     * @param cardNumber полный номер карты (16 цифр)
     * @return текущий баланс карты
     * @throws CardNotFoundException     если карта с указанным номером не найдена
     * @throws CardAccessDeniedException если карта не принадлежит пользователю
     */
    BigDecimal getCardBalance(Long userId, String cardNumber);

    /**
     * Выполняет перевод средств между двумя картами пользователя.
     *
     * <p>Операция выполняется в транзакции с пессимистической блокировкой строк
     * ({@code SELECT FOR UPDATE}) для предотвращения race condition.
     * Карты блокируются в порядке возрастания зашифрованного номера
     * для предотвращения deadlock при параллельных переводах.
     *
     * @param userId идентификатор пользователя
     * @param dto    данные перевода: номера карт отправителя и получателя, сумма
     * @throws InvalidTransferException  если карты отправителя и получателя совпадают
     * @throws CardNotFoundException     если одна из карт не найдена
     * @throws CardAccessDeniedException если одна из карт не принадлежит пользователю
     * @throws CardStatusException       если одна из карт не активна
     * @throws InsufficientFundsException если баланс карты-отправителя недостаточен
     */
    void transferBetweenAccount(Long userId, CardTransferRequestDto dto);

    /**
     * Отправляет запрос на блокировку активной карты пользователя.
     *
     * @param userId     идентификатор пользователя
     * @param cardNumber полный номер карты (16 цифр)
     * @return DTO карты со статусом {@code BLOCKED}
     * @throws CardNotFoundException     если карта с указанным номером не найдена
     * @throws CardAccessDeniedException если карта не принадлежит пользователю
     * @throws CardStatusException       если карта не активна
     */
    CardResponseDto requestBlock(Long userId, String cardNumber);
}
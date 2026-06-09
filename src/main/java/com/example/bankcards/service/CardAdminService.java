package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardCreateRequestDto;
import com.example.bankcards.dto.card.CardResponseDto;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;

/**
 * Сервис административного управления банковскими картами.
 *
 * <p>Предоставляет операции доступные только пользователям с ролью {@code ROLE_ADMIN}:
 * создание, блокировка, активация, удаление карт и просмотр всех карт в системе.
 */
public interface CardAdminService {
    /**
     * Создаёт новую банковскую карту для указанного пользователя.
     *
     * <p>Генерирует уникальный номер карты, шифрует его перед сохранением в БД.
     * При коллизии номеров генерация повторяется до получения уникального значения.
     *
     * <p>Возвращает DTO с незамаскированным номером карты — единственный момент
     * когда полный номер доступен через API.
     *
     * @param dto данные для создания карты: имя владельца и срок действия
     * @return DTO созданной карты с полным (незамаскированным) номером
     * @throws com.example.bankcards.exception.UserNotFoundException если пользователь с указанным именем не найден
     */
    CardResponseDto createCard(CardCreateRequestDto dto);

    /**
     * Возвращает постраничный список карт с опциональной фильтрацией.
     *
     * <p>Оба фильтра необязательны: если {@code ownerName} равен {@code null} —
     * возвращаются карты всех пользователей; если {@code status} равен {@code null} —
     * возвращаются карты с любым статусом.
     *
     * @param ownerName имя владельца карты для фильтрации, {@code null} — без фильтра
     * @param status    статус карты для фильтрации, {@code null} — без фильтра
     * @param pageable  параметры пагинации и сортировки
     * @return страница карт с замаскированными номерами
     */
    PagedModel<CardResponseDto> getAllCards(String ownerName, CardStatus status, Pageable pageable);

    /**
     * Блокирует карту по её полному номеру.
     *
     * <p>Если карта уже заблокирована — возвращает её текущее состояние без изменений.
     *
     * @param cardNumber полный номер карты (16 цифр)
     * @return DTO карты с обновлённым статусом
     * @throws com.example.bankcards.exception.CardNotFoundException если карта с указанным номером не найдена
     * @throws com.example.bankcards.exception.CardStatusException если карта имеет статус {@code EXPIRED}
     */
    CardResponseDto blockCard(String cardNumber);

    /**
     * Активирует заблокированную карту по её полному номеру.
     *
     * <p>Если карта уже активна — возвращает её текущее состояние без изменений.
     *
     * @param cardNumber полный номер карты (16 цифр)
     * @return DTO карты с обновлённым статусом
     * @throws com.example.bankcards.exception.CardNotFoundException если карта с указанным номером не найдена
     * @throws com.example.bankcards.exception.CardStatusException если срок действия карты истёк
     */
    CardResponseDto activateCard(String cardNumber);

    /**
     * Удаляет карту из системы по её полному номеру.
     *
     * @param cardNumber полный номер карты (16 цифр)
     * @throws com.example.bankcards.exception.CardNotFoundException если карта с указанным номером не найдена
     */
    void deleteCard(String cardNumber);
}

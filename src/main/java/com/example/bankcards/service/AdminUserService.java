package com.example.bankcards.service;

import com.example.bankcards.dto.user.UserResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;

/**
 * Сервис административного управления пользователями.
 *
 * <p>Предоставляет операции доступные только пользователям с ролью {@code ROLE_ADMIN}:
 * просмотр списка пользователей, получение информации о конкретном пользователе
 * и удаление пользователей с проверкой бизнес-ограничений.
 */
public interface AdminUserService {

    /**
     * Возвращает постраничный список всех пользователей системы.
     *
     * @param pageable параметры пагинации и сортировки
     * @return страница пользователей
     */
    PagedModel<UserResponseDto> getAllUsers(Pageable pageable);

    /**
     * Удаляет пользователя по имени вместе со всеми его картами.
     *
     * <p>Удаление запрещено в двух случаях:
     * <ul>
     *   <li>пользователь имеет роль {@code ROLE_ADMIN}</li>
     *   <li>на любой из карт пользователя есть положительный баланс</li>
     * </ul>
     *
     * @param username имя пользователя
     * @throws com.example.bankcards.exception.UserNotFoundException
     * если пользователь с указанным именем не найден
     *
     * @throws com.example.bankcards.exception.IllegalOperationException
     * если пользователь является администратором
     * или имеет карты с положительным балансом
     */
    void deleteUserByName(String username);

    /**
     * Возвращает данные пользователя по его имени.
     *
     * @param username имя пользователя
     * @return DTO пользователя
     * @throws com.example.bankcards.exception.UserNotFoundException если пользователь с указанным именем не найден
     */
    UserResponseDto getUserByName(String username);
}
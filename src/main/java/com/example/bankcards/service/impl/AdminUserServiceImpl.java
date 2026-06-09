package com.example.bankcards.service.impl;

import com.example.bankcards.dto.user.UserResponseDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.IllegalOperationException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.AdminUserService;
import com.example.bankcards.util.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedModel<UserResponseDto> getAllUsers(Pageable pageable) {
        log.debug("Fetching users page={} size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<UserResponseDto> page = userRepository.findAll(pageable)
                .map(userMapper::toDto);

        return new PagedModel<>(page);
    }

    @Override
    @Transactional
    public void deleteUserByName(String username) {
        log.warn("Deleting user '{}'", username);

        User user = userRepository.findWithRelationsByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        validateUserCanBeDeleted(user);

        userRepository.delete(user);

        log.info("User '{}' deleted successfully", username);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserByName(String username) {
        log.debug("Fetching user '{}'", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        return userMapper.toDto(user);
    }

    private static void validateUserCanBeDeleted(User user) {

        if (user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {

            throw new IllegalOperationException(
                    "Administrator account cannot be deleted"
            );
        }

        boolean hasMoney = user.getCards()
                .stream()
                .anyMatch(card ->
                        card.getBalance().compareTo(BigDecimal.ZERO) > 0);

        if (hasMoney) {
            throw new IllegalOperationException(
                    "Cannot delete user with positive card balances"
            );
        }
    }
}
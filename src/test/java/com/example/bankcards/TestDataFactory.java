package com.example.bankcards;

import com.example.bankcards.dto.card.CardResponseDto;
import com.example.bankcards.dto.user.UserResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    // Roles

    public static Role roleUser() {
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_USER");
        return role;
    }

    public static Role roleAdmin() {
        Role role = new Role();
        role.setId(2L);
        role.setName("ROLE_ADMIN");
        return role;
    }

    // Users

    public static User adminUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("$2a$10$encodedAdminPassword");
        user.setRoles(Set.of(roleAdmin()));
        user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return user;
    }

    public static User regularUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("$2a$10$encodedPassword");
        user.setRoles(Set.of(roleUser()));
        user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return user;
    }

    public static User regularUser() {
        return regularUser(2L, "testuser");
    }

    // Cards

    public static CardResponseDto cardDto(Long id, String cardNumber, Long ownerId, String ownerUsername,
                                          CardStatus status, BigDecimal balance) {
        return new CardResponseDto(id, cardNumber, ownerId, ownerUsername,
                LocalDate.now().plusYears(2), status, balance,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    public static Card activeCard(Long id, User owner, BigDecimal balance) {
        Card card = new Card();
        card.setId(id);
        card.setEncryptedNumber("encrypted_" + id);
        card.setOwner(owner);
        card.setExpiryDate(LocalDate.now().plusYears(2));
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(balance);
        card.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        card.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return card;
    }

    public static Card blockedCard(Long id, User owner, BigDecimal balance) {
        Card card = activeCard(id, owner, balance);
        card.setStatus(CardStatus.BLOCKED);
        return card;
    }

    // DTOs

    public static UserResponseDto userDto(Long id, String username, Set<String> roles) {
        return new UserResponseDto(
                id,
                username,
                roles,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    public static UserResponseDto userDto(String username) {
        return userDto(2L, username, Set.of("ROLE_USER"));
    }
}
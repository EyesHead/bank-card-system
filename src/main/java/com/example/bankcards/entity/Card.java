package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards", indexes = {
        @Index(name = "idx_cards_owner_id", columnList = "owner_id"),
        @Index(name = "idx_cards_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "encrypted_number", nullable = false, unique = true)
    private String encryptedNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CardStatus status = CardStatus.ACTIVE;

    @Column(name = "balance", nullable = false,
            precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- Бизнес-методы прямо в entity ---
    // Логика принадлежащая самому объекту живёт здесь,
    // а не размазывается по сервисам

    public boolean isActive() {
        return status == CardStatus.ACTIVE;
    }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDate.now());
    }

    public boolean isOwnedBy(Long userId) {
        return owner.getId().equals(userId);
    }

    public void block() {
        this.status = CardStatus.BLOCKED;
    }

    public void activate() {
        this.status = CardStatus.ACTIVE;
    }

    // Метод перевода — @Transactional на уровне Service
    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Недостаточно средств");
        }
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }
        this.balance = this.balance.add(amount);
    }
}

package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c WHERE c.encryptedNumber = :encryptedNumber")
    Optional<Card> findByEncryptedNumberWithLock(@Param("encryptedNumber") String encrypterNumber);

    @Query("""
            SELECT c FROM Card c
            WHERE (c.owner.id = :ownerId)
            AND (:status IS NULL OR c.status = :status)
            """)
    Page<Card> findByUserIdAndStatus(
            @Param("ownerId") Long userId,
            @Param("status") CardStatus status,
            Pageable pageable
    );

    @Query("""
    SELECT c FROM Card c
    WHERE (:username IS NULL OR c.owner.username = :username)
      AND (:status  IS NULL OR c.status   = :status)
    """)
    Page<Card> findAllByOwnerNameAndStatus(
            @Param("username") String username,
            @Param("status") CardStatus status,
            Pageable pageable
    );

    Optional<Card> findByEncryptedNumber(String encryptedNumber);
}

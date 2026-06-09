package com.example.bankcards.service.impl;

import com.example.bankcards.dto.card.CardResponseDto;
import com.example.bankcards.dto.card.CardTransferRequestDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.service.CardUserService;
import com.example.bankcards.util.CardEncryptionService;
import com.example.bankcards.util.CardMaskingService;
import com.example.bankcards.util.mapper.CardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
@Service
public class CardUserServiceImpl implements CardUserService {

    private final CardRepository cardRepository;
    private final CardMapper mapper;
    private final CardEncryptionService encryptionService;
    private final CardMaskingService maskingService;

    @Override
    @Transactional(readOnly = true)
    public PagedModel<CardResponseDto> getMyCards(Long userId, CardStatus status, Pageable pageable) {
        Page<CardResponseDto> cardResponseDtoPage = cardRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(mapper::toUnmaskedDto);
        return new PagedModel<>(cardResponseDtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCardBalance(Long userId, String cardNumber) {
        String encryptNumber = encryptionService.encrypt(cardNumber);

        Card card = cardRepository.findByEncryptedNumber(encryptNumber)
                .orElseThrow(() -> new CardNotFoundException(
                        "Card with cardNumber = '%s' was not found".formatted(maskingService.mask(cardNumber)))
                );
        validateOwnership(card, userId);

        return card.getBalance();
    }

    @Override
    @Transactional
    public void transferBetweenAccount(Long userId, CardTransferRequestDto dto) {
        if (dto.fromCardNumber().equals(dto.toCardNumber())) {
            throw new InvalidTransferException("Source and target cards must be different");
        }

        Card fromCard = findCardWithLockOrThrow(dto.fromCardNumber());
        Card toCard = findCardWithLockOrThrow(dto.toCardNumber());

        validateOwnership(fromCard, userId);
        validateOwnership(toCard, userId);

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardStatusException(
                    "Source card %s is not active. Current status: %s"
                            .formatted(maskingService.mask(dto.fromCardNumber()), fromCard.getStatus())
            );
        }

        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardStatusException(
                    "Target card %s is not active. Current status: %s"
                            .formatted(maskingService.mask(dto.toCardNumber()), toCard.getStatus())
            );
        }

        if (fromCard.getBalance().compareTo(dto.amount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds on card %s".formatted(maskingService.mask(dto.fromCardNumber()))
            );
        }

        fromCard.setBalance(fromCard.getBalance().subtract(dto.amount()));
        toCard.setBalance(toCard.getBalance().add(dto.amount()));

        log.info("Transfer completed: fromCard={}, toCard={}, amount={}, userId={}",
                maskingService.mask(dto.fromCardNumber()), maskingService.mask(dto.toCardNumber()), dto.amount(), userId);
    }

    @Override
    @Transactional
    public CardResponseDto requestBlock(Long userId, String cardNumber) {
        Card card = findCardWithLockOrThrow(cardNumber);

        validateOwnership(card, userId);

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new CardStatusException("Card %s is not active, current status: %s"
                    .formatted(maskingService.mask(cardNumber), card.getStatus())
            );
        }

        card.setStatus(CardStatus.BLOCKED);
        CardResponseDto cardResponseForUserDto = mapper.toUnmaskedDto(card);

        log.info("Card {} was blocked by user '{}'", maskingService.mask(cardNumber), userId);

        return cardResponseForUserDto;
    }

    private void validateOwnership(Card card, Long userId) {
        Long ownerId = card.getOwner().getId();

        if (!ownerId.equals(userId)) {
            throw new CardAccessDeniedException(
                    "Card %s does not belong to user with id='%s'"
                            .formatted(maskingService.maskFromEncrypted(card.getEncryptedNumber()), userId)
            );
        }
    }

    /**
     * Вспомогательный метод для поиска карты в БД с пессимистической блокировкой (SELECT FOR UPDATE)
     */
    private Card findCardWithLockOrThrow(String rawCardNumber) {
        String encrypted = encryptionService.encrypt(rawCardNumber);
        return cardRepository.findByEncryptedNumberWithLock(encrypted)
                .orElseThrow(() -> new CardNotFoundException(
                        "Card with cardNumber = '%s' was not found".formatted(maskingService.mask(rawCardNumber)))
                );
    }
}
package com.example.bankcards.service.impl;

import com.example.bankcards.dto.card.CardCreateRequestDto;
import com.example.bankcards.dto.card.CardResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.CardStatusException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardAdminService;
import com.example.bankcards.util.CardEncryptionService;
import com.example.bankcards.util.CardMaskingService;
import com.example.bankcards.util.CardNumberGenerator;
import com.example.bankcards.util.mapper.CardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@RequiredArgsConstructor
@Service
public class CardAdminServiceImpl implements CardAdminService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;
    private final CardEncryptionService encryptionService;
    private final CardNumberGenerator cardNumberGenerator;
    private final CardMaskingService maskingService;

    @Override
    @Transactional
    public CardResponseDto createCard(CardCreateRequestDto dto) {
        User owner = userRepository.findByUsername(dto.username())
                .orElseThrow(() -> new UserNotFoundException("User with username='%s' not found".formatted(dto.username())));

        String cardNumberGenerated;
        String encryptedCardNumber;
        boolean isDuplicate;

        do {
            cardNumberGenerated = cardNumberGenerator.generate();
            encryptedCardNumber = encryptionService.encrypt(cardNumberGenerated);
            isDuplicate = cardRepository.findByEncryptedNumber(encryptedCardNumber).isPresent();
        } while (isDuplicate);

        Card card = Card.builder()
                .encryptedNumber(encryptedCardNumber)
                .owner(owner)
                .expiryDate(dto.expiryDate())
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build();

        Card saved = cardRepository.save(card);
        log.info("Card successfully created: id={} (masked={}) for userId={}",
                saved.getId(), maskingService.mask(cardNumberGenerated), owner.getId());

        return cardMapper.toUnmaskedDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedModel<CardResponseDto> getAllCards(String ownerName, CardStatus status, Pageable pageable) {
        Page<CardResponseDto> pageableResponse = cardRepository.findAllByOwnerNameAndStatus(ownerName, status, pageable)
                .map(cardMapper::toMaskedDto);
        return new PagedModel<>(pageableResponse);
    }

    @Override
    @Transactional
    public CardResponseDto blockCard(String cardNumber) {
        Card card = findCardByRawNumberOrThrow(cardNumber);
        String masked = maskingService.mask(cardNumber);

        switch (card.getStatus()) {
            case BLOCKED -> {
                return cardMapper.toMaskedDto(card);
            }
            case EXPIRED -> throw new CardStatusException(
                    "Cannot block expired card '%s'".formatted(masked));
        }

        card.setStatus(CardStatus.BLOCKED);
        log.info("Card '{}' blocked by admin", masked);

        return cardMapper.toMaskedDto(card);
    }

    @Override
    @Transactional
    public CardResponseDto activateCard(String cardNumber) {
        Card card = findCardByRawNumberOrThrow(cardNumber);
        String masked = maskingService.mask(cardNumber);

        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new CardStatusException(
                    "Cannot activate card '%s' because it is expired (expiryDate=%s)"
                            .formatted(masked, card.getExpiryDate()));
        }

        if (card.getStatus() == CardStatus.ACTIVE) {
            return cardMapper.toMaskedDto(card);
        }

        card.setStatus(CardStatus.ACTIVE);
        log.info("Card '{}' activated by admin", masked);

        return cardMapper.toMaskedDto(card);
    }

    @Override
    @Transactional
    public void deleteCard(String cardNumber) {
        Card card = findCardByRawNumberOrThrow(cardNumber);
        cardRepository.delete(card);
        log.info("Card '{}' deleted by admin", maskingService.mask(cardNumber));
    }

    /**
     * Ищет карту по её полному номеру, предварительно зашифровав его для сравнения в БД
     */
    private Card findCardByRawNumberOrThrow(String rawCardNumber) {
        String encrypted = encryptionService.encrypt(rawCardNumber);
        return cardRepository.findByEncryptedNumber(encrypted)
                .orElseThrow(() -> new CardNotFoundException(
                        "Card with cardNumber = '%s' not found".formatted(maskingService.mask(rawCardNumber))));
    }
}
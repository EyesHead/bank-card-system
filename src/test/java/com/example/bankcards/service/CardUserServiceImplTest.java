package com.example.bankcards.service;

import com.example.bankcards.TestDataFactory;
import com.example.bankcards.dto.card.CardResponseDto;
import com.example.bankcards.dto.card.CardTransferRequestDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.service.impl.CardUserServiceImpl;
import com.example.bankcards.util.CardEncryptionService;
import com.example.bankcards.util.CardMaskingService;
import com.example.bankcards.util.mapper.CardMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardUserServiceImplTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private CardMapper mapper;
    @Mock
    private CardEncryptionService encryptionService;
    @Mock
    private CardMaskingService maskingService;

    @InjectMocks
    private CardUserServiceImpl cardUserService;

    private final User owner = TestDataFactory.regularUser(2L, "testuser");
    private final User otherUser = TestDataFactory.regularUser(3L, "other");
    private static final String ENC_FROM = "enc_from";
    private static final String ENC_TO = "enc_to";
    private static final String ENC_CARD = "enc_1";

    private Card createCard(Long id, User owner, BigDecimal balance, String encryptedNumber) {
        Card card = Card.builder()
                .encryptedNumber(encryptedNumber)
                .owner(owner)
                .status(CardStatus.ACTIVE)
                .balance(balance)
                .expiryDate(LocalDate.now().plusYears(3))
                .build();
        card.setId(id);
        return card;
    }

    void getMyCards_shouldReturnPagedResult() {
        Card card = TestDataFactory.activeCard(1L, owner, BigDecimal.valueOf(1000));
        Page<Card> page = new PageImpl<>(List.of(card));
        CardResponseDto dto = TestDataFactory.cardDto(
                1L, "1234123412341000", 2L, "testuser",
                CardStatus.ACTIVE, BigDecimal.valueOf(1000));

        when(cardRepository.findByUserIdAndStatus(eq(2L), eq(CardStatus.ACTIVE), any(PageRequest.class)))
                .thenReturn(page);
        when(mapper.toUnmaskedDto(card)).thenReturn(dto);

        PagedModel<CardResponseDto> result = cardUserService.getMyCards(2L, CardStatus.ACTIVE, PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals("1234123412341000", result.getContent().get(0).cardNumber());
        assertEquals(BigDecimal.valueOf(1000), result.getContent().get(0).balance());
    }

    @Test
    void getCardBalance_shouldReturnBalance() {
        Card card = createCard(1L, owner, BigDecimal.valueOf(500), ENC_CARD);

        when(encryptionService.encrypt("1234567890123456")).thenReturn(ENC_CARD);
        when(cardRepository.findByEncryptedNumber(ENC_CARD)).thenReturn(Optional.of(card));
        // maskFromEncrypted НЕ вызывается, так как владелец совпадает → стаб не нужен

        BigDecimal balance = cardUserService.getCardBalance(2L, "1234567890123456");
        assertEquals(BigDecimal.valueOf(500), balance);
    }

    @Test
    void getCardBalance_shouldThrowWhenNotOwner() {
        Card card = createCard(1L, otherUser, BigDecimal.valueOf(500), ENC_CARD);

        when(encryptionService.encrypt("1234567890123456")).thenReturn(ENC_CARD);
        when(cardRepository.findByEncryptedNumber(ENC_CARD)).thenReturn(Optional.of(card));
        // maskFromEncrypted ВЫЗЫВАЕТСЯ при ошибке → стаб нужен
        when(maskingService.maskFromEncrypted(ENC_CARD)).thenReturn("****3456");

        assertThrows(CardAccessDeniedException.class,
                () -> cardUserService.getCardBalance(2L, "1234567890123456"));
    }

    @Test
    void getCardBalance_shouldThrowWhenCardNotFound() {
        when(encryptionService.encrypt("0000000000000000")).thenReturn(ENC_CARD);
        when(cardRepository.findByEncryptedNumber(ENC_CARD)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class,
                () -> cardUserService.getCardBalance(2L, "0000000000000000"));
    }

    @Test
    void transferBetweenAccount_shouldSucceed() {
        Card fromCard = createCard(1L, owner, BigDecimal.valueOf(1000), ENC_FROM);
        Card toCard = createCard(2L, owner, BigDecimal.valueOf(500), ENC_TO);

        CardTransferRequestDto dto = new CardTransferRequestDto(
                "1111111111111111", "2222222222222222", BigDecimal.valueOf(200));

        when(encryptionService.encrypt("1111111111111111")).thenReturn(ENC_FROM);
        when(encryptionService.encrypt("2222222222222222")).thenReturn(ENC_TO);
        when(cardRepository.findByEncryptedNumberWithLock(ENC_FROM)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByEncryptedNumberWithLock(ENC_TO)).thenReturn(Optional.of(toCard));
        // maskFromEncrypted НЕ вызывается (владельцы совпадают) → стабы не нужны
        // (Опционально) стаб для лога
        when(maskingService.mask(anyString())).thenReturn("****masked");

        cardUserService.transferBetweenAccount(2L, dto);

        assertEquals(BigDecimal.valueOf(800), fromCard.getBalance());
        assertEquals(BigDecimal.valueOf(700), toCard.getBalance());
    }

    @Test
    void transferBetweenAccount_shouldThrowWhenSameCard() {
        CardTransferRequestDto dto = new CardTransferRequestDto(
                "1111111111111111", "1111111111111111", BigDecimal.valueOf(100));

        assertThrows(InvalidTransferException.class,
                () -> cardUserService.transferBetweenAccount(2L, dto));
    }

    @Test
    void transferBetweenAccount_shouldThrowWhenInsufficientFunds() {
        Card fromCard = createCard(1L, owner, BigDecimal.valueOf(50), ENC_FROM);
        Card toCard = createCard(2L, owner, BigDecimal.valueOf(500), ENC_TO);

        CardTransferRequestDto dto = new CardTransferRequestDto(
                "1111111111111111", "2222222222222222", BigDecimal.valueOf(200));

        when(encryptionService.encrypt("1111111111111111")).thenReturn(ENC_FROM);
        when(encryptionService.encrypt("2222222222222222")).thenReturn(ENC_TO);
        when(cardRepository.findByEncryptedNumberWithLock(ENC_FROM)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByEncryptedNumberWithLock(ENC_TO)).thenReturn(Optional.of(toCard));
        // maskFromEncrypted не нужен
        when(maskingService.mask(anyString())).thenReturn("****masked");

        assertThrows(InsufficientFundsException.class,
                () -> cardUserService.transferBetweenAccount(2L, dto));
    }

    @Test
    void transferBetweenAccount_shouldThrowWhenFromCardBlocked() {
        Card fromCard = createCard(1L, owner, BigDecimal.valueOf(1000), ENC_FROM);
        fromCard.setStatus(CardStatus.BLOCKED);
        Card toCard = createCard(2L, owner, BigDecimal.valueOf(500), ENC_TO);

        CardTransferRequestDto dto = new CardTransferRequestDto(
                "1111111111111111", "2222222222222222", BigDecimal.valueOf(200));

        when(encryptionService.encrypt("1111111111111111")).thenReturn(ENC_FROM);
        when(encryptionService.encrypt("2222222222222222")).thenReturn(ENC_TO);
        when(cardRepository.findByEncryptedNumberWithLock(ENC_FROM)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByEncryptedNumberWithLock(ENC_TO)).thenReturn(Optional.of(toCard));
        // maskFromEncrypted не нужен
        when(maskingService.mask(anyString())).thenReturn("****masked");

        assertThrows(CardStatusException.class,
                () -> cardUserService.transferBetweenAccount(2L, dto));
    }

    @Test
    void requestBlock_shouldBlockCard() {
        Card card = createCard(1L, owner, BigDecimal.valueOf(100), ENC_CARD);
        CardResponseDto dto = TestDataFactory.cardDto(
                1L, "1234123412341111", 2L, "testuser",
                CardStatus.BLOCKED, BigDecimal.valueOf(100));

        when(encryptionService.encrypt("1111111111111111")).thenReturn(ENC_CARD);
        when(cardRepository.findByEncryptedNumberWithLock(ENC_CARD)).thenReturn(Optional.of(card));
        when(mapper.toUnmaskedDto(card)).thenReturn(dto);
        when(maskingService.mask(anyString())).thenReturn("****1111");

        CardResponseDto result = cardUserService.requestBlock(2L, "1111111111111111");

        assertEquals(CardStatus.BLOCKED, card.getStatus());
        assertEquals(CardStatus.BLOCKED, result.status());
    }

    @Test
    void requestBlock_shouldThrowWhenNotActive() {
        Card card = createCard(1L, owner, BigDecimal.valueOf(100), ENC_CARD);
        card.setStatus(CardStatus.BLOCKED);

        when(encryptionService.encrypt("1111111111111111")).thenReturn(ENC_CARD);
        when(cardRepository.findByEncryptedNumberWithLock(ENC_CARD)).thenReturn(Optional.of(card));
        when(maskingService.mask(anyString())).thenReturn("****1111");
        // maskFromEncrypted не нужен

        assertThrows(CardStatusException.class,
                () -> cardUserService.requestBlock(2L, "1111111111111111"));
    }

    @Test
    void requestBlock_shouldThrowWhenNotOwner() {
        Card card = createCard(1L, otherUser, BigDecimal.valueOf(100), ENC_CARD);

        when(encryptionService.encrypt("1111111111111111")).thenReturn(ENC_CARD);
        when(cardRepository.findByEncryptedNumberWithLock(ENC_CARD)).thenReturn(Optional.of(card));
        // maskFromEncrypted ВЫЗЫВАЕТСЯ при ошибке → стаб нужен
        when(maskingService.maskFromEncrypted(ENC_CARD)).thenReturn("****1111");

        assertThrows(CardAccessDeniedException.class,
                () -> cardUserService.requestBlock(2L, "1111111111111111"));
    }
}
package com.example.bankcards.service;

import com.example.bankcards.TestDataFactory;
import com.example.bankcards.dto.card.CardCreateRequestDto;
import com.example.bankcards.dto.card.CardResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.impl.CardAdminServiceImpl;
import com.example.bankcards.util.CardEncryptionService;
import com.example.bankcards.util.CardMaskingService;
import com.example.bankcards.util.CardNumberGenerator;
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
class CardAdminServiceImplTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CardMapper cardMapper;
    @Mock
    private CardEncryptionService encryptionService;
    @Mock
    private CardNumberGenerator cardNumberGenerator;
    @Mock
    private CardMaskingService maskingService;

    @InjectMocks
    private CardAdminServiceImpl cardAdminService;

    private final User owner = TestDataFactory.regularUser(2L, "testuser");

    @Test
    void createCard_shouldCreateAndReturnDto() {
        CardCreateRequestDto dto = new CardCreateRequestDto("testuser", LocalDate.now().plusYears(3));
        Card savedCard = TestDataFactory.activeCard(1L, owner, BigDecimal.ZERO);
        CardResponseDto expectedDto = TestDataFactory.cardDto(
                1L, "1234567890125678", 2L, "testuser", CardStatus.ACTIVE, BigDecimal.ZERO);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(owner));
        when(cardNumberGenerator.generate()).thenReturn("1234567890125678");
        when(encryptionService.encrypt("1234567890125678")).thenReturn("encrypted_1");
        when(cardRepository.findByEncryptedNumber("encrypted_1")).thenReturn(Optional.empty());
        when(cardRepository.save(any(Card.class))).thenReturn(savedCard);
        when(cardMapper.toUnmaskedDto(savedCard)).thenReturn(expectedDto);  // unmasked при создании
        when(maskingService.mask("1234567890125678")).thenReturn("****5678");

        CardResponseDto result = cardAdminService.createCard(dto);

        assertNotNull(result);
        assertEquals("1234567890125678", result.cardNumber());
        assertEquals(CardStatus.ACTIVE, result.status());
        assertEquals(BigDecimal.ZERO, result.balance());
    }

    @Test
    void createCard_shouldThrowWhenUserNotFound() {
        CardCreateRequestDto dto = new CardCreateRequestDto("unknown", LocalDate.now().plusYears(3));

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> cardAdminService.createCard(dto));
    }

    @Test
    void getAllCards_shouldReturnPagedResult() {
        Card card = TestDataFactory.activeCard(1L, owner, BigDecimal.valueOf(1000));
        Page<Card> page = new PageImpl<>(List.of(card));
        CardResponseDto dto = TestDataFactory.cardDto(1L, "****1000", 2L, "testuser",
                CardStatus.ACTIVE, BigDecimal.valueOf(1000));

        when(cardRepository.findAllByOwnerNameAndStatus(eq("testuser"), eq(CardStatus.ACTIVE), any(PageRequest.class)))
                .thenReturn(page);
        when(cardMapper.toMaskedDto(card)).thenReturn(dto);

        PagedModel<CardResponseDto> result = cardAdminService.getAllCards("testuser", CardStatus.ACTIVE, PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals("testuser", result.getContent().get(0).ownerUsername());
    }

    @Test
    void blockCard_shouldBlockActiveCard() {
        Card card = TestDataFactory.activeCard(1L, owner, BigDecimal.valueOf(500));
        CardResponseDto dto = TestDataFactory.cardDto(1L, "****1111", 2L, "testuser",
                CardStatus.BLOCKED, BigDecimal.valueOf(500));

        when(encryptionService.encrypt("1111111111111111")).thenReturn("enc_1");
        when(cardRepository.findByEncryptedNumber("enc_1")).thenReturn(Optional.of(card));
        when(maskingService.mask("1111111111111111")).thenReturn("****1111");
        when(cardMapper.toMaskedDto(card)).thenReturn(dto);

        CardResponseDto result = cardAdminService.blockCard("1111111111111111");

        assertEquals(CardStatus.BLOCKED, card.getStatus());
        assertEquals(CardStatus.BLOCKED, result.status());
    }

    @Test
    void blockCard_shouldReturnSameDtoWhenAlreadyBlocked() {
        Card card = TestDataFactory.blockedCard(1L, owner, BigDecimal.valueOf(500));
        CardResponseDto dto = TestDataFactory.cardDto(1L, "****1111", 2L, "testuser",
                CardStatus.BLOCKED, BigDecimal.valueOf(500));

        when(encryptionService.encrypt("1111111111111111")).thenReturn("enc_1");
        when(cardRepository.findByEncryptedNumber("enc_1")).thenReturn(Optional.of(card));
        when(maskingService.mask("1111111111111111")).thenReturn("****1111");
        when(cardMapper.toMaskedDto(card)).thenReturn(dto);

        CardResponseDto result = cardAdminService.blockCard("1111111111111111");

        assertEquals(CardStatus.BLOCKED, result.status());
        verify(cardRepository, never()).save(any());
    }

    @Test
    void blockCard_shouldThrowWhenCardNotFound() {
        when(encryptionService.encrypt("0000000000000000")).thenReturn("enc_not_found");
        when(cardRepository.findByEncryptedNumber("enc_not_found")).thenReturn(Optional.empty());
        when(maskingService.mask("0000000000000000")).thenReturn("****0000");

        assertThrows(CardNotFoundException.class,
                () -> cardAdminService.blockCard("0000000000000000"));
    }

    @Test
    void activateCard_shouldActivateBlockedCard() {
        Card card = TestDataFactory.blockedCard(1L, owner, BigDecimal.valueOf(500));
        CardResponseDto dto = TestDataFactory.cardDto(1L, "****1111", 2L, "testuser",
                CardStatus.ACTIVE, BigDecimal.valueOf(500));

        when(encryptionService.encrypt("1111111111111111")).thenReturn("enc_1");
        when(cardRepository.findByEncryptedNumber("enc_1")).thenReturn(Optional.of(card));
        when(maskingService.mask("1111111111111111")).thenReturn("****1111");
        when(cardMapper.toMaskedDto(card)).thenReturn(dto);

        CardResponseDto result = cardAdminService.activateCard("1111111111111111");

        assertEquals(CardStatus.ACTIVE, card.getStatus());
        assertEquals(CardStatus.ACTIVE, result.status());
    }

    @Test
    void deleteCard_shouldDeleteExistingCard() {
        Card card = TestDataFactory.activeCard(1L, owner, BigDecimal.ZERO);

        when(encryptionService.encrypt("1111111111111111")).thenReturn("enc_1");
        when(cardRepository.findByEncryptedNumber("enc_1")).thenReturn(Optional.of(card));
        when(maskingService.mask("1111111111111111")).thenReturn("****1111");

        cardAdminService.deleteCard("1111111111111111");

        verify(cardRepository).delete(card);
    }

    @Test
    void deleteCard_shouldThrowWhenCardNotFound() {
        when(encryptionService.encrypt("0000000000000000")).thenReturn("enc_not_found");
        when(cardRepository.findByEncryptedNumber("enc_not_found")).thenReturn(Optional.empty());
        when(maskingService.mask("0000000000000000")).thenReturn("****0000");

        assertThrows(CardNotFoundException.class,
                () -> cardAdminService.deleteCard("0000000000000000"));
    }
}
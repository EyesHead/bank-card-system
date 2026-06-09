package com.example.bankcards.util.mapper;

import com.example.bankcards.dto.card.CardResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.util.CardEncryptionService;
import com.example.bankcards.util.CardMaskingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardMapperImpl implements CardMapper {
    private final CardMaskingService maskingService;
    private final CardEncryptionService encryptionService;

    @Override
    public CardResponseDto toMaskedDto(Card card) {
        return new CardResponseDto(
                card.getId(),
                maskingService.maskFromEncrypted(card.getEncryptedNumber()),
                card.getOwner().getId(),
                card.getOwner().getUsername(),
                card.getExpiryDate(),
                card.getStatus(),
                card.getBalance(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }

    @Override
    public CardResponseDto toUnmaskedDto(Card card) {
        return new CardResponseDto(
                card.getId(),
                encryptionService.decrypt(card.getEncryptedNumber()),
                card.getOwner().getId(),
                card.getOwner().getUsername(),
                card.getExpiryDate(),
                card.getStatus(),
                card.getBalance(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }
}

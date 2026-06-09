package com.example.bankcards.util.mapper;

import com.example.bankcards.dto.card.CardResponseDto;
import com.example.bankcards.entity.Card;

public interface CardMapper {
    CardResponseDto toMaskedDto(Card card);
    CardResponseDto toUnmaskedDto(Card card);
}

package com.example.bankcards.util;

public interface CardMaskingService {
    String mask(String cardNumber);
    String maskFromEncrypted(String encryptedCardNumber);
}
package com.example.bankcards.util;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class StandardCardMaskingService implements CardMaskingService {

    private final CardEncryptionService encryptionService;

    public StandardCardMaskingService(CardEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String mask(String cardNumber) {
        String digitsOnly = cardNumber.replaceAll(" ", "");
        String lastFourDigits = digitsOnly.substring(digitsOnly.length() - 4);
        return "**** **** **** " + lastFourDigits;
    }

    @Override
    public String maskFromEncrypted(String encryptedCardNumber) {
        String realCardNumber = encryptionService.decrypt(encryptedCardNumber);
        return mask(realCardNumber);
    }
}
package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Primary
@Service
public class AesCardEncryptionService implements CardEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    @Value("${application.security.encryption.cardNumber.secret-key}")
    private String secretKey;

    @Override
    public String encrypt(String cardNumber) {
        try {
            SecretKeySpec key = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "AES"
            );

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encryptedBytes = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось зашифровать номер карты (ECB)", e);
        }
    }

    @Override
    public String decrypt(String encryptedCardNumber) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedCardNumber);

            SecretKeySpec key = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "AES"
            );

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось расшифровать номер карты (ECB)", e);        }
    }
}
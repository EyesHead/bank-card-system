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

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    @Value("${application.security.encryption.cardNumber.secret-key}")
    private String secretKey;

    @Override
    public String encrypt(String cardNumber) {
        try {
            SecretKeySpec key = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "AES"
            );

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

            byte[] encryptedBytes = cipher.doFinal(
                    cardNumber.getBytes(StandardCharsets.UTF_8)
            );

            byte[] ivPlusEncrypted = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, ivPlusEncrypted, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, ivPlusEncrypted, iv.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(ivPlusEncrypted);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось зашифровать номер карты", e);
        }
    }

    @Override
    public String decrypt(String encryptedCardNumber) {
        try {
            byte[] ivPlusEncrypted = Base64.getDecoder().decode(encryptedCardNumber);

            byte[] iv = new byte[16];
            byte[] encryptedBytes = new byte[ivPlusEncrypted.length - 16];
            System.arraycopy(ivPlusEncrypted, 0, iv, 0, 16);
            System.arraycopy(ivPlusEncrypted, 16, encryptedBytes, 0, encryptedBytes.length);

            SecretKeySpec key = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "AES"
            );

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Не удалось расшифровать номер карты", e);
        }
    }
}
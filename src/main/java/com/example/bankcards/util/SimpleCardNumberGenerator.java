package com.example.bankcards.util;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class SimpleCardNumberGenerator implements CardNumberGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(16);
        sb.append(1 + RANDOM.nextInt(9));
        for (int i = 1; i < 16; i++) {
            sb.append(RANDOM.nextInt(10));
        }

        return sb.toString();
    }
}

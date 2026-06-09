package com.example.bankcards.exception;

import org.springframework.http.HttpStatus;

public class CardStatusException extends ApiException {
    public CardStatusException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}

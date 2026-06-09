package com.example.bankcards.exception;

import org.springframework.http.HttpStatus;

public class CardAccessDeniedException extends ApiException {
    public CardAccessDeniedException(String s) {
        super(s, HttpStatus.FORBIDDEN);
    }
}

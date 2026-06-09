package com.example.bankcards.exception;

import org.springframework.http.HttpStatus;

public class IllegalOperationException extends ApiException {
    public IllegalOperationException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
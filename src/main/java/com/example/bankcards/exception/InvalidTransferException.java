package com.example.bankcards.exception;

import org.springframework.http.HttpStatus;

public class InvalidTransferException extends ApiException {
    public InvalidTransferException(String s) {
        super(s, HttpStatus.BAD_REQUEST);
    }
}

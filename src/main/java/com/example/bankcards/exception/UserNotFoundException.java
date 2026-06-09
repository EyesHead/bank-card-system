package com.example.bankcards.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException(String s) {
        super(s, HttpStatus.NOT_FOUND);
    }
}

package com.example.bankcards.exception;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        LocalDateTime timeStamp,
        Integer status,
        String error,
        String message,
        String path
) {}

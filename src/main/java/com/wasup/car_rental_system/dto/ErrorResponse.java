package com.wasup.car_rental_system.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String error,
        LocalDateTime timestamp
) {
    public ErrorResponse(int status, String error) {
        this(status, error, LocalDateTime.now());
    }
}

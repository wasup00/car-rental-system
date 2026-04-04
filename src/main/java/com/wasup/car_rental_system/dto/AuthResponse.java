package com.wasup.car_rental_system.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {}

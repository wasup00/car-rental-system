package com.wasup.car_rental_system.dto;

public record AvailabilityResponse(
        long available,
        long total
) {}

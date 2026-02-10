package com.wasup.car_rental_system.dto;

import com.wasup.car_rental_system.model.CarType;

import java.time.LocalDateTime;

public record ReservationRequest(
        CarType carType,
        String customerName,
        LocalDateTime startDateTime,
        int numberOfDays
) {}

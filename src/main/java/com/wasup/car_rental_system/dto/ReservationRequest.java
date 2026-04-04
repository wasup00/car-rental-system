package com.wasup.car_rental_system.dto;

import com.wasup.car_rental_system.model.CarType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservationRequest(
        @NotNull CarType carType,
        @NotNull @FutureOrPresent LocalDateTime startDateTime,
        @NotNull @Min(1) Integer numberOfDays
) {}

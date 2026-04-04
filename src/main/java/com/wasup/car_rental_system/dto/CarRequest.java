package com.wasup.car_rental_system.dto;

import com.wasup.car_rental_system.model.CarType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CarRequest(
        @NotNull CarType type,
        @NotBlank String licensePlate
) {}

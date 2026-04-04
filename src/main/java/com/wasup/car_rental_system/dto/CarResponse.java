package com.wasup.car_rental_system.dto;

import com.wasup.car_rental_system.model.Car;
import com.wasup.car_rental_system.model.CarType;

public record CarResponse(
        String id,
        CarType type,
        String licensePlate,
        String tenantId,
        String tenantName
) {
    public static CarResponse fromEntity(Car car) {
        return new CarResponse(
                car.getId(),
                car.getType(),
                car.getLicensePlate(),
                car.getTenant().getId(),
                car.getTenant().getName()
        );
    }
}

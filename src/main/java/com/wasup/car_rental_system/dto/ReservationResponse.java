package com.wasup.car_rental_system.dto;

import com.wasup.car_rental_system.model.CarType;
import com.wasup.car_rental_system.model.Reservation;

import java.time.LocalDateTime;

public record ReservationResponse(
        String id,
        String carId,
        String licensePlate,
        CarType carType,
        String customerName,
        LocalDateTime startDateTime,
        int numberOfDays
) {
    public static ReservationResponse fromEntity(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getCar().getId(),
                reservation.getCar().getLicensePlate(),
                reservation.getCarType(),
                reservation.getCustomerName(),
                reservation.getStartDateTime(),
                reservation.getNumberOfDays()
        );
    }
}

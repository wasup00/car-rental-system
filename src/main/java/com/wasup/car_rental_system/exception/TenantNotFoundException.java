package com.wasup.car_rental_system.exception;

import org.springframework.http.HttpStatus;

public class TenantNotFoundException extends CarRentalException {
    public TenantNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}

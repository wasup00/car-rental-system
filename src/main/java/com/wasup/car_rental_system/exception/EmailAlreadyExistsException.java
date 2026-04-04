package com.wasup.car_rental_system.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends CarRentalException {
    public EmailAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}

package com.wasup.car_rental_system.exception;

import org.springframework.http.HttpStatus;

public abstract class CarRentalException extends RuntimeException {

    private final HttpStatus status;

    protected CarRentalException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

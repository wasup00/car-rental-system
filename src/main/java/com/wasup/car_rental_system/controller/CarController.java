package com.wasup.car_rental_system.controller;

import com.wasup.car_rental_system.dto.AvailabilityResponse;
import com.wasup.car_rental_system.model.Car;
import com.wasup.car_rental_system.model.CarType;
import com.wasup.car_rental_system.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @RequestParam("type") CarType type,
            @RequestParam("startDate") LocalDateTime startDate,
            @RequestParam("days") int days) {
        return ResponseEntity.ok(carService.getAvailability(type, startDate, days));
    }

    @GetMapping
    public ResponseEntity<List<Car>> getAllCars() {
        return ResponseEntity.ok(carService.getAllCars());
    }
}

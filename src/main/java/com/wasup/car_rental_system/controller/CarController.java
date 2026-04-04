package com.wasup.car_rental_system.controller;

import com.wasup.car_rental_system.dto.AvailabilityResponse;
import com.wasup.car_rental_system.dto.CarRequest;
import com.wasup.car_rental_system.dto.CarResponse;
import com.wasup.car_rental_system.model.CarType;
import com.wasup.car_rental_system.security.TenantContext;
import com.wasup.car_rental_system.service.AvailabilityService;
import com.wasup.car_rental_system.service.CarService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
@Validated
public class CarController {

    private final CarService carService;
    private final AvailabilityService availabilityService;

    @GetMapping
    public ResponseEntity<List<CarResponse>> getAllCars() {
        return ResponseEntity.ok(carService.getAllCars().stream()
                .map(CarResponse::fromEntity)
                .toList());
    }

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @RequestParam("type") CarType type,
            @RequestParam("startDate") LocalDateTime startDate,
            @RequestParam("days") @Min(1) int days) {
        String tenantId = TenantContext.currentTenantId();
        return ResponseEntity.ok(availabilityService.getAvailability(type, startDate, days, tenantId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<CarResponse> createCar(@Valid @RequestBody CarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CarResponse.fromEntity(carService.createCar(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<Void> deleteCar(@PathVariable String id) {
        carService.deleteCar(id);
        return ResponseEntity.noContent().build();
    }
}

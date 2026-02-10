package com.wasup.car_rental_system.config;

import com.wasup.car_rental_system.model.Car;
import com.wasup.car_rental_system.model.CarType;
import com.wasup.car_rental_system.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Map<CarType, Integer> FLEET_COUNTS = Map.of(
            CarType.SEDAN, 5,
            CarType.SUV, 3,
            CarType.VAN, 2
    );

    private final CarRepository carRepository;

    @Override
    public void run(String... args) {
        if (carRepository.count() > 0) {
            return;
        }

        FLEET_COUNTS.forEach((type, count) -> {
            for (int i = 1; i <= count; i++) {
                String plate = type.name() + "-" + String.format("%03d", i);
                carRepository.save(Car.builder().type(type).licensePlate(plate).build());
            }
        });
    }
}

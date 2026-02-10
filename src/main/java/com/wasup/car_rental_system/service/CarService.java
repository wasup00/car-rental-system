package com.wasup.car_rental_system.service;

import com.wasup.car_rental_system.dto.AvailabilityResponse;
import com.wasup.car_rental_system.model.Car;
import com.wasup.car_rental_system.model.CarType;
import com.wasup.car_rental_system.model.Reservation;
import com.wasup.car_rental_system.repository.CarRepository;
import com.wasup.car_rental_system.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final ReservationRepository reservationRepository;

    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    public List<Car> getCarsByType(CarType carType) {
        return carRepository.findByType(carType);
    }

    public AvailabilityResponse getAvailability(CarType carType, LocalDateTime startDate, int days) {
        LocalDateTime requestEnd = startDate.plusDays(days);

        List<Car> carsOfType = carRepository.findByType(carType);

        long total = carsOfType.size();
        long available = carsOfType.stream()
                .filter(car -> reservationRepository.findByCar(car).stream()
                        .noneMatch(r -> overlaps(r, startDate, requestEnd)))
                .count();

        return new AvailabilityResponse(available, total);
    }

    boolean overlaps(Reservation existing, LocalDateTime newStart, LocalDateTime newEnd) {
        LocalDateTime existingStart = existing.getStartDateTime();
        LocalDateTime existingEnd = existingStart.plusDays(existing.getNumberOfDays());
        return existingStart.isBefore(newEnd) && newStart.isBefore(existingEnd);
    }
}
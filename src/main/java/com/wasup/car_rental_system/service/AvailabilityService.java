package com.wasup.car_rental_system.service;

import com.wasup.car_rental_system.dto.AvailabilityResponse;
import com.wasup.car_rental_system.model.Car;
import com.wasup.car_rental_system.model.CarType;
import com.wasup.car_rental_system.repository.CarRepository;
import com.wasup.car_rental_system.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final CarRepository carRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(CarType carType, LocalDateTime startDate, int days, String tenantId) {
        LocalDateTime requestEnd = startDate.plusDays(days);
        List<Car> carsOfType = carRepository.findByTypeAndTenantId(carType, tenantId);

        long total = carsOfType.size();
        long available = carsOfType.stream()
                .filter(car -> isCarAvailable(car, startDate, requestEnd))
                .count();

        return new AvailabilityResponse(available, total);
    }

    @Transactional(readOnly = true)
    public Optional<Car> findAvailableCar(CarType carType, LocalDateTime startDate, int days, String tenantId) {
        LocalDateTime requestEnd = startDate.plusDays(days);
        return carRepository.findByTypeAndTenantId(carType, tenantId).stream()
                .filter(car -> isCarAvailable(car, startDate, requestEnd))
                .findFirst();
    }

    private boolean isCarAvailable(Car car, LocalDateTime start, LocalDateTime end) {
        return reservationRepository.findByCar(car).stream()
                .noneMatch(r -> r.overlaps(start, end));
    }
}

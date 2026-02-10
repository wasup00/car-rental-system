package com.wasup.car_rental_system.service;

import com.wasup.car_rental_system.dto.ReservationRequest;
import com.wasup.car_rental_system.dto.ReservationResponse;
import com.wasup.car_rental_system.exception.CarNotAvailableException;
import com.wasup.car_rental_system.exception.ReservationNotFoundException;
import com.wasup.car_rental_system.model.Car;
import com.wasup.car_rental_system.model.Reservation;
import com.wasup.car_rental_system.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final CarService carService;
    private final ReservationRepository reservationRepository;

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        LocalDateTime requestStart = request.startDateTime();
        LocalDateTime requestEnd = requestStart.plusDays(request.numberOfDays());

        List<Car> carsOfType = carService.getCarsByType(request.carType());

        for (Car car : carsOfType) {
            List<Reservation> carReservations = reservationRepository.findByCar(car);
            boolean isAvailable = carReservations.stream()
                    .noneMatch(r -> carService.overlaps(r, requestStart, requestEnd));

            if (isAvailable) {
                Reservation reservation = Reservation.builder()
                        .car(car)
                        .carType(request.carType())
                        .customerName(request.customerName())
                        .startDateTime(request.startDateTime())
                        .numberOfDays(request.numberOfDays())
                        .build();
                return ReservationResponse.fromEntity(reservationRepository.save(reservation));
            }
        }

        throw new CarNotAvailableException(
                "No " + request.carType() + " available for the requested dates");
    }

    @Transactional
    public void cancelReservation(String id) {
        if (!reservationRepository.existsById(id)) {
            throw new ReservationNotFoundException("Reservation with id " + id + " not found");
        }
        reservationRepository.deleteById(id);
    }

    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(ReservationResponse::fromEntity)
                .toList();
    }
}
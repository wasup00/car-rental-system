package com.wasup.car_rental_system.service;

import com.wasup.car_rental_system.dto.ReservationRequest;
import com.wasup.car_rental_system.dto.ReservationResponse;
import com.wasup.car_rental_system.exception.CarNotAvailableException;
import com.wasup.car_rental_system.model.Car;
import com.wasup.car_rental_system.model.CarType;
import com.wasup.car_rental_system.repository.CarRepository;
import com.wasup.car_rental_system.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class ReservationServiceTest {

    @Autowired
    private ReservationService service;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private final LocalDateTime baseDate = LocalDateTime.of(2025, 6, 1, 10, 0);

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        carRepository.deleteAll();

        for (int i = 1; i <= 5; i++) {
            carRepository.save(Car.builder()
                    .type(CarType.SEDAN).licensePlate("SEDAN-" + String.format("%03d", i)).build());
        }
        for (int i = 1; i <= 3; i++) {
            carRepository.save(Car.builder()
                    .type(CarType.SUV).licensePlate("SUV-" + String.format("%03d", i)).build());
        }
        for (int i = 1; i <= 2; i++) {
            carRepository.save(Car.builder()
                    .type(CarType.VAN).licensePlate("VAN-" + String.format("%03d", i)).build());
        }
    }

    @Test
    void createReservationSuccessfully() {
        ReservationRequest request = new ReservationRequest(
                CarType.SEDAN, "Alice", baseDate, 3);

        ReservationResponse response = service.createReservation(request);

        assertNotNull(response);
        assertNotNull(response.id());
        assertNotNull(response.carId());
        assertNotNull(response.licensePlate());
        assertEquals(CarType.SEDAN, response.carType());
        assertEquals("Alice", response.customerName());
        assertEquals(baseDate, response.startDateTime());
        assertEquals(3, response.numberOfDays());
    }

    @Test
    void bookingAllCarsPreventsFurtherBookingsForOverlappingDates() {
        for (int i = 0; i < 5; i++) {
            ReservationRequest request = new ReservationRequest(
                    CarType.SEDAN, "Customer " + i, baseDate, 3);
            service.createReservation(request);
        }

        ReservationRequest request = new ReservationRequest(
                CarType.SEDAN, "Extra Customer", baseDate.plusDays(1), 1);
        assertThrows(CarNotAvailableException.class,
                () -> service.createReservation(request));
    }

    @Test
    void nonOverlappingDatesAllowBookingWhenFullyBookedForOtherDates() {
        for (int i = 0; i < 5; i++) {
            ReservationRequest request = new ReservationRequest(
                    CarType.SEDAN, "Customer " + i, baseDate, 3);
            service.createReservation(request);
        }

        ReservationRequest request = new ReservationRequest(
                CarType.SEDAN, "Late Customer", baseDate.plusDays(3), 3);
        ReservationResponse response = service.createReservation(request);

        assertNotNull(response);
        assertEquals("Late Customer", response.customerName());
    }

    @Test
    void cancellationFreesUpCarForRebooking() {
        ReservationResponse first = service.createReservation(
                new ReservationRequest(CarType.VAN, "Van Customer 1", baseDate, 5));
        service.createReservation(
                new ReservationRequest(CarType.VAN, "Van Customer 2", baseDate, 5));

        assertThrows(CarNotAvailableException.class,
                () -> service.createReservation(
                        new ReservationRequest(CarType.VAN, "Van Customer 3", baseDate, 5)));

        service.cancelReservation(first.id());

        ReservationResponse rebooking = service.createReservation(
                new ReservationRequest(CarType.VAN, "Van Customer 3", baseDate, 5));
        assertNotNull(rebooking);
        assertEquals(CarType.VAN, rebooking.carType());
    }

    @Test
    void eachCarTypeWorksIndependently() {
        for (int i = 0; i < 5; i++) {
            service.createReservation(
                    new ReservationRequest(CarType.SEDAN, "Sedan " + i, baseDate, 3));
        }

        ReservationResponse suv = service.createReservation(
                new ReservationRequest(CarType.SUV, "SUV Customer", baseDate, 3));
        assertNotNull(suv);
        assertEquals(CarType.SUV, suv.carType());

        ReservationResponse van = service.createReservation(
                new ReservationRequest(CarType.VAN, "VAN Customer", baseDate, 3));
        assertNotNull(van);
        assertEquals(CarType.VAN, van.carType());

        assertThrows(CarNotAvailableException.class,
                () -> service.createReservation(
                        new ReservationRequest(CarType.SEDAN, "Extra Sedan", baseDate, 3)));
    }
}

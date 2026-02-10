package com.wasup.car_rental_system.repository;

import com.wasup.car_rental_system.model.Car;
import com.wasup.car_rental_system.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, String> {
    List<Reservation> findByCar(Car car);
}

package com.wasup.car_rental_system.service;

import com.wasup.car_rental_system.dto.ReservationRequest;
import com.wasup.car_rental_system.dto.ReservationResponse;
import com.wasup.car_rental_system.exception.CarNotAvailableException;
import com.wasup.car_rental_system.exception.ReservationNotFoundException;
import com.wasup.car_rental_system.model.Car;
import com.wasup.car_rental_system.model.Reservation;
import com.wasup.car_rental_system.model.Role;
import com.wasup.car_rental_system.model.Tenant;
import com.wasup.car_rental_system.model.User;
import com.wasup.car_rental_system.repository.ReservationRepository;
import com.wasup.car_rental_system.repository.TenantRepository;
import com.wasup.car_rental_system.repository.UserRepository;
import com.wasup.car_rental_system.security.TenantContext;
import com.wasup.car_rental_system.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final AvailabilityService availabilityService;
    private final ReservationRepository reservationRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        UserPrincipal currentUser = TenantContext.currentUser();
        String tenantId = currentUser.tenantId();

        Car car = availabilityService
                .findAvailableCar(request.carType(), request.startDateTime(), request.numberOfDays(), tenantId)
                .orElseThrow(() -> new CarNotAvailableException(
                        "No " + request.carType() + " available for the requested dates"));

        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        User user = userRepository.findById(currentUser.id()).orElseThrow();

        Reservation reservation = Reservation.builder()
                .car(car)
                .tenant(tenant)
                .user(user)
                .carType(request.carType())
                .customerName(user.getFullName())
                .startDateTime(request.startDateTime())
                .numberOfDays(request.numberOfDays())
                .build();

        return ReservationResponse.fromEntity(reservationRepository.save(reservation));
    }

    @Transactional
    public void cancelReservation(String id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation with id " + id + " not found"));

        UserPrincipal currentUser = TenantContext.currentUser();
        Role role = currentUser.role();

        boolean allowed = switch (role) {
            case ADMIN -> true;
            case CLIENT -> reservation.getTenant().getId().equals(currentUser.tenantId());
            case CUSTOMER -> reservation.getUser().getId().equals(currentUser.id());
        };

        if (!allowed) {
            throw new AccessDeniedException("Not authorized to cancel this reservation");
        }

        reservationRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllReservations() {
        UserPrincipal currentUser = TenantContext.currentUser();
        return switch (currentUser.role()) {
            case ADMIN -> reservationRepository.findAll().stream()
                    .map(ReservationResponse::fromEntity)
                    .toList();
            case CLIENT -> reservationRepository.findByTenantId(currentUser.tenantId()).stream()
                    .map(ReservationResponse::fromEntity)
                    .toList();
            case CUSTOMER -> reservationRepository.findByUserId(currentUser.id()).stream()
                    .map(ReservationResponse::fromEntity)
                    .toList();
        };
    }
}

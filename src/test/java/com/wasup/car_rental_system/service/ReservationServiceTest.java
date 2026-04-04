package com.wasup.car_rental_system.service;

import com.wasup.car_rental_system.dto.ReservationRequest;
import com.wasup.car_rental_system.dto.ReservationResponse;
import com.wasup.car_rental_system.exception.CarNotAvailableException;
import com.wasup.car_rental_system.model.*;
import com.wasup.car_rental_system.repository.*;
import com.wasup.car_rental_system.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ReservationServiceTest {

    @Autowired
    private ReservationService service;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final LocalDateTime baseDate = LocalDateTime.of(2025, 6, 1, 10, 0);
    private Tenant tenant;
    private User testUser;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant")
                .slug("test")
                .active(true)
                .build());

        testUser = userRepository.save(User.builder()
                .email("test@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .fullName("Test User")
                .role(Role.CUSTOMER)
                .tenant(tenant)
                .build());

        for (int i = 1; i <= 5; i++) {
            carRepository.save(Car.builder()
                    .type(CarType.SEDAN).licensePlate("SEDAN-" + String.format("%03d", i)).tenant(tenant).build());
        }
        for (int i = 1; i <= 3; i++) {
            carRepository.save(Car.builder()
                    .type(CarType.SUV).licensePlate("SUV-" + String.format("%03d", i)).tenant(tenant).build());
        }
        for (int i = 1; i <= 2; i++) {
            carRepository.save(Car.builder()
                    .type(CarType.VAN).licensePlate("VAN-" + String.format("%03d", i)).tenant(tenant).build());
        }

        // Set up security context with the test user's actual IDs
        UserPrincipal principal = new UserPrincipal(
                testUser.getId(), testUser.getEmail(), testUser.getFullName(),
                tenant.getId(), Role.CUSTOMER, null);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReservationSuccessfully() {
        ReservationRequest request = new ReservationRequest(CarType.SEDAN, baseDate, 3);

        ReservationResponse response = service.createReservation(request);

        assertNotNull(response);
        assertNotNull(response.id());
        assertNotNull(response.carId());
        assertNotNull(response.licensePlate());
        assertEquals(CarType.SEDAN, response.carType());
        assertEquals("Test User", response.customerName());
        assertEquals(baseDate, response.startDateTime());
        assertEquals(3, response.numberOfDays());
    }

    @Test
    void bookingAllCarsPreventsFurtherBookingsForOverlappingDates() {
        for (int i = 0; i < 5; i++) {
            service.createReservation(new ReservationRequest(CarType.SEDAN, baseDate, 3));
        }

        assertThrows(CarNotAvailableException.class,
                () -> service.createReservation(new ReservationRequest(CarType.SEDAN, baseDate.plusDays(1), 1)));
    }

    @Test
    void nonOverlappingDatesAllowBookingWhenFullyBookedForOtherDates() {
        for (int i = 0; i < 5; i++) {
            service.createReservation(new ReservationRequest(CarType.SEDAN, baseDate, 3));
        }

        ReservationResponse response = service.createReservation(
                new ReservationRequest(CarType.SEDAN, baseDate.plusDays(3), 3));

        assertNotNull(response);
        assertEquals("Test User", response.customerName());
    }

    @Test
    void cancellationFreesUpCarForRebooking() {
        ReservationResponse first = service.createReservation(new ReservationRequest(CarType.VAN, baseDate, 5));
        service.createReservation(new ReservationRequest(CarType.VAN, baseDate, 5));

        assertThrows(CarNotAvailableException.class,
                () -> service.createReservation(new ReservationRequest(CarType.VAN, baseDate, 5)));

        service.cancelReservation(first.id());

        ReservationResponse rebooking = service.createReservation(new ReservationRequest(CarType.VAN, baseDate, 5));
        assertNotNull(rebooking);
        assertEquals(CarType.VAN, rebooking.carType());
    }

    @Test
    void eachCarTypeWorksIndependently() {
        for (int i = 0; i < 5; i++) {
            service.createReservation(new ReservationRequest(CarType.SEDAN, baseDate, 3));
        }

        ReservationResponse suv = service.createReservation(new ReservationRequest(CarType.SUV, baseDate, 3));
        assertNotNull(suv);
        assertEquals(CarType.SUV, suv.carType());

        ReservationResponse van = service.createReservation(new ReservationRequest(CarType.VAN, baseDate, 3));
        assertNotNull(van);
        assertEquals(CarType.VAN, van.carType());

        assertThrows(CarNotAvailableException.class,
                () -> service.createReservation(new ReservationRequest(CarType.SEDAN, baseDate, 3)));
    }
}

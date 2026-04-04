package com.wasup.car_rental_system.controller;

import com.wasup.car_rental_system.model.*;
import com.wasup.car_rental_system.repository.*;
import com.wasup.car_rental_system.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CarControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    private Authentication auth;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant")
                .slug("test")
                .active(true)
                .build());

        User user = userRepository.save(User.builder()
                .email("test@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .fullName("Test User")
                .role(Role.CUSTOMER)
                .tenant(tenant)
                .build());

        carRepository.save(Car.builder().type(CarType.SEDAN).licensePlate("SEDAN-001").tenant(tenant).build());
        carRepository.save(Car.builder().type(CarType.SEDAN).licensePlate("SEDAN-002").tenant(tenant).build());
        carRepository.save(Car.builder().type(CarType.SUV).licensePlate("SUV-001").tenant(tenant).build());

        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getEmail(), user.getFullName(),
                tenant.getId(), Role.CUSTOMER, null);
        auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void getAllCars_returnsAllCars() throws Exception {
        mockMvc.perform(get("/cars").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].licensePlate", containsInAnyOrder("SEDAN-001", "SEDAN-002", "SUV-001")));
    }

    @Test
    void getAvailability_allAvailable() throws Exception {
        mockMvc.perform(get("/cars/availability").with(authentication(auth))
                        .param("type", "SEDAN")
                        .param("startDate", "2027-07-01T10:00:00")
                        .param("days", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(2))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void getAvailability_singleSuvIsAvailable() throws Exception {
        mockMvc.perform(get("/cars/availability").with(authentication(auth))
                        .param("type", "SUV")
                        .param("startDate", "2027-07-01T10:00:00")
                        .param("days", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(1))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void getCars_returns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/cars"))
                .andExpect(status().isUnauthorized());
    }
}

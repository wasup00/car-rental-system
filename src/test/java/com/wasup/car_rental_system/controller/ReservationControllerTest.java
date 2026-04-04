package com.wasup.car_rental_system.controller;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.ObjectMapper;
import com.wasup.car_rental_system.dto.ReservationRequest;
import com.wasup.car_rental_system.model.*;
import com.wasup.car_rental_system.repository.*;
import com.wasup.car_rental_system.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private final LocalDateTime baseDate = LocalDateTime.of(2027, 7, 1, 10, 0);

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
                .email("alice@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .fullName("Alice")
                .role(Role.CUSTOMER)
                .tenant(tenant)
                .build());

        carRepository.save(Car.builder().type(CarType.SEDAN).licensePlate("SEDAN-001").tenant(tenant).build());
        carRepository.save(Car.builder().type(CarType.VAN).licensePlate("VAN-001").tenant(tenant).build());

        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getEmail(), user.getFullName(),
                tenant.getId(), Role.CUSTOMER, null);
        auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void createReservation_returns201WithResponse() throws Exception {
        ReservationRequest request = new ReservationRequest(CarType.SEDAN, baseDate, 3);

        mockMvc.perform(post("/reservations").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.carId").isNotEmpty())
                .andExpect(jsonPath("$.licensePlate").value("SEDAN-001"))
                .andExpect(jsonPath("$.carType").value("SEDAN"))
                .andExpect(jsonPath("$.customerName").value("Alice"))
                .andExpect(jsonPath("$.numberOfDays").value(3));
    }

    @Test
    void createReservation_returns409WhenNoCarsAvailable() throws Exception {
        ReservationRequest first = new ReservationRequest(CarType.SEDAN, baseDate, 3);
        mockMvc.perform(post("/reservations").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        ReservationRequest second = new ReservationRequest(CarType.SEDAN, baseDate.plusDays(1), 2);
        mockMvc.perform(post("/reservations").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("No SEDAN available")));
    }

    @Test
    void getAllReservations_returnsEmptyListInitially() throws Exception {
        mockMvc.perform(get("/reservations").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllReservations_returnsCreatedReservations() throws Exception {
        ReservationRequest request = new ReservationRequest(CarType.SEDAN, baseDate, 3);
        mockMvc.perform(post("/reservations").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/reservations").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerName").value("Alice"))
                .andExpect(jsonPath("$[0].carType").value("SEDAN"));
    }

    @Test
    void cancelReservation_returns204() throws Exception {
        ReservationRequest request = new ReservationRequest(CarType.VAN, baseDate, 5);
        MvcResult result = mockMvc.perform(post("/reservations").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();

        mockMvc.perform(delete("/reservations/{id}", id).with(authentication(auth)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/reservations").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void cancelReservation_returns404ForUnknownId() throws Exception {
        mockMvc.perform(delete("/reservations/{id}", "non-existent-id").with(authentication(auth)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("not found")));
    }

    @Test
    void createReservation_returns400WhenNumberOfDaysIsZero() throws Exception {
        ReservationRequest request = new ReservationRequest(CarType.SEDAN, baseDate, 0);
        mockMvc.perform(post("/reservations").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createReservation_returns400WhenStartDateIsInPast() throws Exception {
        ReservationRequest request = new ReservationRequest(
                CarType.SEDAN, LocalDateTime.of(2020, 1, 1, 10, 0), 3);
        mockMvc.perform(post("/reservations").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}

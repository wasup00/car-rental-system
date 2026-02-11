package com.wasup.car_rental_system.controller;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.ObjectMapper;
import com.wasup.car_rental_system.dto.ReservationRequest;
import com.wasup.car_rental_system.model.Car;
import com.wasup.car_rental_system.model.CarType;
import com.wasup.car_rental_system.repository.CarRepository;
import com.wasup.car_rental_system.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
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

    private final LocalDateTime baseDate = LocalDateTime.of(2025, 7, 1, 10, 0);

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        carRepository.deleteAll();

        carRepository.save(Car.builder().type(CarType.SEDAN).licensePlate("SEDAN-001").build());
        carRepository.save(Car.builder().type(CarType.VAN).licensePlate("VAN-001").build());
    }

    @Test
    void createReservation_returns201WithResponse() throws Exception {
        ReservationRequest request = new ReservationRequest(CarType.SEDAN, "Alice", baseDate, 3);

        mockMvc.perform(post("/reservations")
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
        // Book the only sedan
        ReservationRequest first = new ReservationRequest(CarType.SEDAN, "Alice", baseDate, 3);
        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        // Try to book another sedan for overlapping dates
        ReservationRequest second = new ReservationRequest(CarType.SEDAN, "Bob", baseDate.plusDays(1), 2);
        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("No SEDAN available")));
    }

    @Test
    void getAllReservations_returnsEmptyListInitially() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllReservations_returnsCreatedReservations() throws Exception {
        ReservationRequest request = new ReservationRequest(CarType.SEDAN, "Alice", baseDate, 3);
        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerName").value("Alice"))
                .andExpect(jsonPath("$[0].carType").value("SEDAN"));
    }

    @Test
    void cancelReservation_returns204() throws Exception {
        // Create a reservation first
        ReservationRequest request = new ReservationRequest(CarType.VAN, "Charlie", baseDate, 5);
        MvcResult result = mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();

        // Cancel it
        mockMvc.perform(delete("/reservations/{id}", id))
                .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void cancelReservation_returns404ForUnknownId() throws Exception {
        mockMvc.perform(delete("/reservations/{id}", "non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("not found")));
    }
}

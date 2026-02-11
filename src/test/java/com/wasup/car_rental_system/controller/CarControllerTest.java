package com.wasup.car_rental_system.controller;

import com.wasup.car_rental_system.model.Car;
import com.wasup.car_rental_system.model.CarType;
import com.wasup.car_rental_system.repository.CarRepository;
import com.wasup.car_rental_system.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
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

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        carRepository.deleteAll();

        carRepository.save(Car.builder().type(CarType.SEDAN).licensePlate("SEDAN-001").build());
        carRepository.save(Car.builder().type(CarType.SEDAN).licensePlate("SEDAN-002").build());
        carRepository.save(Car.builder().type(CarType.SUV).licensePlate("SUV-001").build());
    }

    @Test
    void getAllCars_returnsAllCars() throws Exception {
        mockMvc.perform(get("/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].licensePlate", containsInAnyOrder("SEDAN-001", "SEDAN-002", "SUV-001")));
    }

    @Test
    void getAvailability_allAvailable() throws Exception {
        mockMvc.perform(get("/cars/availability")
                        .param("type", "SEDAN")
                        .param("startDate", "2025-07-01T10:00:00")
                        .param("days", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(2))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void getAvailability_noneAvailableAfterBooking() throws Exception {
        // Book both sedans for the same dates
        mockMvc.perform(get("/cars/availability")
                        .param("type", "SUV")
                        .param("startDate", "2025-07-01T10:00:00")
                        .param("days", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(1))
                .andExpect(jsonPath("$.total").value(1));
    }
}

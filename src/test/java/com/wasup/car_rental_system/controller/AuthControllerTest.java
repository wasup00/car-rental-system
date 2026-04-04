package com.wasup.car_rental_system.controller;

import tools.jackson.databind.ObjectMapper;
import com.wasup.car_rental_system.dto.LoginRequest;
import com.wasup.car_rental_system.dto.RegisterRequest;
import com.wasup.car_rental_system.model.*;
import com.wasup.car_rental_system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private CarRepository carRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name("Auth Test Tenant")
                .slug("auth-test")
                .active(true)
                .build());

        userRepository.save(User.builder()
                .email("user@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .fullName("Test User")
                .role(Role.CUSTOMER)
                .tenant(tenant)
                .build());
    }

    @Test
    void login_returns200WithTokens() throws Exception {
        LoginRequest request = new LoginRequest("user@test.com", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("user@test.com"));
    }

    @Test
    void login_returns401WithBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest("user@test.com", "wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_returns201WithTokens() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "newuser@test.com", "securePass1", "New User", "auth-test");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("newuser@test.com"));
    }

    @Test
    void register_returns409WhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "user@test.com", "password123", "Dup User", "auth-test");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void listTenants_returns200WithActiveTenants() throws Exception {
        mockMvc.perform(get("/auth/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("auth-test"));
    }

    @Test
    void refresh_returns200WithNewTokens() throws Exception {
        LoginRequest loginRequest = new LoginRequest("user@test.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("refreshToken").asText();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.wasup.car_rental_system.dto.RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void jwtToken_authenticatesRequestToProtectedEndpoint() throws Exception {
        LoginRequest loginRequest = new LoginRequest("user@test.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void register_returns404WithUnknownTenantSlug() throws Exception {
        com.wasup.car_rental_system.dto.RegisterRequest request = new com.wasup.car_rental_system.dto.RegisterRequest(
                "nobody@test.com", "password123", "Nobody", "unknown-slug");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}

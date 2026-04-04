package com.wasup.car_rental_system.controller;

import tools.jackson.databind.ObjectMapper;
import com.wasup.car_rental_system.dto.CreateTenantRequest;
import com.wasup.car_rental_system.model.*;
import com.wasup.car_rental_system.repository.*;
import com.wasup.car_rental_system.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private CarRepository carRepository;

    private Authentication adminAuth;
    private Authentication customerAuth;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        UserPrincipal adminPrincipal = new UserPrincipal(
                "admin-id", "admin@system.com", "Admin", null, Role.ADMIN, null);
        adminAuth = new UsernamePasswordAuthenticationToken(
                adminPrincipal, null, adminPrincipal.getAuthorities());

        UserPrincipal customerPrincipal = new UserPrincipal(
                "cust-id", "cust@test.com", "Customer", "some-tenant", Role.CUSTOMER, null);
        customerAuth = new UsernamePasswordAuthenticationToken(
                customerPrincipal, null, customerPrincipal.getAuthorities());
    }

    @Test
    void createTenant_returns201AsAdmin() throws Exception {
        CreateTenantRequest request = new CreateTenantRequest(
                "New Corp", "new-corp", "client@newcorp.com", "securePass1", "Corp Client");

        mockMvc.perform(post("/admin/tenants").with(authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("new-corp"))
                .andExpect(jsonPath("$.name").value("New Corp"));
    }

    @Test
    void createTenant_returns403AsCustomer() throws Exception {
        CreateTenantRequest request = new CreateTenantRequest(
                "New Corp", "new-corp", "client@newcorp.com", "securePass1", "Corp Client");

        mockMvc.perform(post("/admin/tenants").with(authentication(customerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllTenants_returns200AsAdmin() throws Exception {
        tenantRepository.save(Tenant.builder()
                .name("Existing Corp").slug("existing").active(true).build());

        mockMvc.perform(get("/admin/tenants").with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("existing"));
    }

    @Test
    void getAllUsers_returns200AsAdmin() throws Exception {
        mockMvc.perform(get("/admin/users").with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

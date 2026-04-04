package com.wasup.car_rental_system.controller;

import com.wasup.car_rental_system.dto.AuthResponse;
import com.wasup.car_rental_system.dto.LoginRequest;
import com.wasup.car_rental_system.dto.RefreshRequest;
import com.wasup.car_rental_system.dto.RegisterRequest;
import com.wasup.car_rental_system.repository.TenantRepository;
import com.wasup.car_rental_system.dto.TenantResponse;
import com.wasup.car_rental_system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TenantRepository tenantRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<TenantResponse>> listTenants() {
        return ResponseEntity.ok(tenantRepository.findAll().stream()
                .filter(t -> t.isActive())
                .map(TenantResponse::fromEntity)
                .toList());
    }
}

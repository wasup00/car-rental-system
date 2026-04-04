package com.wasup.car_rental_system.service;

import com.wasup.car_rental_system.dto.CreateTenantRequest;
import com.wasup.car_rental_system.dto.TenantResponse;
import com.wasup.car_rental_system.dto.UserResponse;
import com.wasup.car_rental_system.exception.EmailAlreadyExistsException;
import com.wasup.car_rental_system.model.Role;
import com.wasup.car_rental_system.model.Tenant;
import com.wasup.car_rental_system.model.User;
import com.wasup.car_rental_system.repository.TenantRepository;
import com.wasup.car_rental_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        if (userRepository.existsByEmail(request.clientEmail())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.clientEmail());
        }

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(request.tenantName())
                .slug(request.tenantSlug())
                .active(true)
                .build());

        userRepository.save(User.builder()
                .email(request.clientEmail())
                .passwordHash(passwordEncoder.encode(request.clientPassword()))
                .fullName(request.clientFullName())
                .role(Role.CLIENT)
                .tenant(tenant)
                .build());

        return TenantResponse.fromEntity(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll().stream()
                .map(TenantResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }
}

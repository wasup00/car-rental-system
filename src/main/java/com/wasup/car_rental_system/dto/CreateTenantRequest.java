package com.wasup.car_rental_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank String tenantName,
        @NotBlank String tenantSlug,
        @NotBlank @Email String clientEmail,
        @NotBlank @Size(min = 6) String clientPassword,
        @NotBlank String clientFullName
) {}

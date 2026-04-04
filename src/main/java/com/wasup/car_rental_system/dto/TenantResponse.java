package com.wasup.car_rental_system.dto;

import com.wasup.car_rental_system.model.Tenant;

public record TenantResponse(
        String id,
        String name,
        String slug,
        boolean active
) {
    public static TenantResponse fromEntity(Tenant tenant) {
        return new TenantResponse(tenant.getId(), tenant.getName(), tenant.getSlug(), tenant.isActive());
    }
}

package com.wasup.car_rental_system.dto;

import com.wasup.car_rental_system.model.Role;
import com.wasup.car_rental_system.model.User;

public record UserResponse(
        String id,
        String email,
        String fullName,
        Role role,
        String tenantId,
        String tenantName
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getTenant() != null ? user.getTenant().getId() : null,
                user.getTenant() != null ? user.getTenant().getName() : null
        );
    }
}

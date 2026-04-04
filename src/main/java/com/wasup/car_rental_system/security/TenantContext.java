package com.wasup.car_rental_system.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class TenantContext {

    private TenantContext() {}

    public static UserPrincipal currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal up) {
            return up;
        }
        throw new IllegalStateException("No authenticated UserPrincipal in SecurityContext");
    }

    public static String currentTenantId() {
        return currentUser().tenantId();
    }

    public static String currentUserId() {
        return currentUser().id();
    }
}

package com.wasup.car_rental_system.security;

import com.wasup.car_rental_system.model.Role;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@WithSecurityContext(factory = WithMockTenantUserSecurityContextFactory.class)
public @interface WithMockTenantUser {
    String id() default "test-user-id";
    String email() default "test@test.com";
    String fullName() default "Test User";
    String tenantId() default "test-tenant-id";
    Role role() default Role.CUSTOMER;
}

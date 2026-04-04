package com.wasup.car_rental_system.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockTenantUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockTenantUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockTenantUser annotation) {
        UserPrincipal principal = new UserPrincipal(
                annotation.id(),
                annotation.email(),
                annotation.fullName(),
                annotation.tenantId(),
                annotation.role(),
                null
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}

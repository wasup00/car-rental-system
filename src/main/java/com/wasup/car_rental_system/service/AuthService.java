package com.wasup.car_rental_system.service;

import com.wasup.car_rental_system.dto.*;
import com.wasup.car_rental_system.exception.EmailAlreadyExistsException;
import com.wasup.car_rental_system.exception.TenantNotFoundException;
import com.wasup.car_rental_system.model.Role;
import com.wasup.car_rental_system.model.Tenant;
import com.wasup.car_rental_system.model.User;
import com.wasup.car_rental_system.repository.TenantRepository;
import com.wasup.car_rental_system.repository.UserRepository;
import com.wasup.car_rental_system.security.CustomUserDetailsService;
import com.wasup.car_rental_system.security.JwtTokenProvider;
import com.wasup.car_rental_system.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return buildResponse(principal);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.email());
        }

        Tenant tenant = tenantRepository.findBySlug(request.tenantSlug())
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + request.tenantSlug()));

        User user = userRepository.save(User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(Role.CUSTOMER)
                .tenant(tenant)
                .build());

        return buildResponse(UserPrincipal.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();
        if (!jwtTokenProvider.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        Claims claims = jwtTokenProvider.parseToken(token);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Not a refresh token");
        }

        String userId = claims.getSubject();
        UserPrincipal principal = userDetailsService.loadUserById(userId);
        return buildResponse(principal);
    }

    private AuthResponse buildResponse(UserPrincipal principal) {
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);
        User user = userRepository.findById(principal.id()).orElseThrow();
        return new AuthResponse(accessToken, refreshToken, UserResponse.fromEntity(user));
    }
}

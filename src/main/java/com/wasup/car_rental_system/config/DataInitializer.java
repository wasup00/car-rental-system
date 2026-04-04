package com.wasup.car_rental_system.config;

import com.wasup.car_rental_system.model.*;
import com.wasup.car_rental_system.repository.CarRepository;
import com.wasup.car_rental_system.repository.TenantRepository;
import com.wasup.car_rental_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Map<CarType, Integer> FLEET_COUNTS = Map.of(
            CarType.SEDAN, 5,
            CarType.SUV, 3,
            CarType.VAN, 2
    );

    private final CarRepository carRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(User.builder()
                    .email("admin@carrental.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .fullName("System Admin")
                    .role(Role.ADMIN)
                    .build());
            log.info("Seeded admin user: admin@carrental.com / admin123");
        }

        if (tenantRepository.count() == 0) {
            Tenant defaultTenant = tenantRepository.save(Tenant.builder()
                    .name("Demo Fleet")
                    .slug("demo")
                    .active(true)
                    .build());

            userRepository.save(User.builder()
                    .email("client@demo.com")
                    .passwordHash(passwordEncoder.encode("client123"))
                    .fullName("Demo Client")
                    .role(Role.CLIENT)
                    .tenant(defaultTenant)
                    .build());

            userRepository.save(User.builder()
                    .email("customer@demo.com")
                    .passwordHash(passwordEncoder.encode("customer123"))
                    .fullName("Demo Customer")
                    .role(Role.CUSTOMER)
                    .tenant(defaultTenant)
                    .build());

            log.info("Seeded tenant 'Demo Fleet' with client@demo.com / client123 and customer@demo.com / customer123");
        }

        if (carRepository.count() == 0) {
            Tenant defaultTenant = tenantRepository.findBySlug("demo").orElseThrow();
            FLEET_COUNTS.forEach((type, count) -> {
                for (int i = 1; i <= count; i++) {
                    String plate = type.name() + "-" + String.format("%03d", i);
                    carRepository.save(Car.builder()
                            .type(type)
                            .licensePlate(plate)
                            .tenant(defaultTenant)
                            .build());
                }
            });
            log.info("Seeded {} cars for tenant 'Demo Fleet'", carRepository.count());
        }
    }
}

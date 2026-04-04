package com.wasup.car_rental_system.service;

import com.wasup.car_rental_system.dto.CarRequest;
import com.wasup.car_rental_system.exception.TenantNotFoundException;
import com.wasup.car_rental_system.model.Car;
import com.wasup.car_rental_system.model.CarType;
import com.wasup.car_rental_system.model.Role;
import com.wasup.car_rental_system.model.Tenant;
import com.wasup.car_rental_system.repository.CarRepository;
import com.wasup.car_rental_system.repository.TenantRepository;
import com.wasup.car_rental_system.security.TenantContext;
import com.wasup.car_rental_system.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public List<Car> getAllCars() {
        UserPrincipal user = TenantContext.currentUser();
        if (user.role() == Role.ADMIN) {
            return carRepository.findAll();
        }
        return carRepository.findByTenantId(user.tenantId());
    }

    @Transactional(readOnly = true)
    public List<Car> getCarsByType(CarType carType) {
        UserPrincipal user = TenantContext.currentUser();
        if (user.role() == Role.ADMIN) {
            return carRepository.findByType(carType);
        }
        return carRepository.findByTypeAndTenantId(carType, user.tenantId());
    }

    @Transactional
    public Car createCar(CarRequest request) {
        UserPrincipal user = TenantContext.currentUser();
        String tenantId = user.tenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        return carRepository.save(Car.builder()
                .type(request.type())
                .licensePlate(request.licensePlate())
                .tenant(tenant)
                .build());
    }

    @Transactional
    public void deleteCar(String carId) {
        carRepository.deleteById(carId);
    }
}

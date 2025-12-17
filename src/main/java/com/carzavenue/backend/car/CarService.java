package com.carzavenue.backend.car;

import com.carzavenue.backend.car.dto.CarRequest;
import com.carzavenue.backend.car.dto.CarResponse;
import com.carzavenue.backend.user.User;
import com.carzavenue.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class CarService {
    private final CarListingRepository carRepository;
    private final UserRepository userRepository;
    private final int vipDefaultDays;

    public CarService(CarListingRepository carRepository, UserRepository userRepository,
                      @org.springframework.beans.factory.annotation.Value("${app.vip.default-days:7}") int vipDefaultDays) {
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.vipDefaultDays = vipDefaultDays;
    }

    @Transactional(readOnly = true)
    public Page<CarResponse> list(String make,
                                  String model,
                                  Integer yearMin,
                                  Integer yearMax,
                                  Double priceMin,
                                  Double priceMax,
                                  String fuelType,
                                  String transmission,
                                  String bodyType,
                                  Integer mileageMin,
                                  Integer mileageMax,
                                  String location,
                                  Boolean isVip,
                                  boolean all,
                                  int page,
                                  int size,
                                  String sort) {
        Specification<CarListing> spec = Specification.where(CarSpecifications.active())
                .and(Optional.ofNullable(make).map(CarSpecifications::make).orElse(null))
                .and(Optional.ofNullable(model).map(CarSpecifications::model).orElse(null))
                .and(Optional.ofNullable(yearMin).map(CarSpecifications::yearMin).orElse(null))
                .and(Optional.ofNullable(yearMax).map(CarSpecifications::yearMax).orElse(null))
                .and(Optional.ofNullable(priceMin).map(CarSpecifications::priceMin).orElse(null))
                .and(Optional.ofNullable(priceMax).map(CarSpecifications::priceMax).orElse(null))
                .and(Optional.ofNullable(fuelType).map(CarSpecifications::fuelType).orElse(null))
                .and(Optional.ofNullable(transmission).map(CarSpecifications::transmission).orElse(null))
                .and(Optional.ofNullable(bodyType).map(CarSpecifications::bodyType).orElse(null))
                .and(Optional.ofNullable(mileageMin).map(CarSpecifications::mileageMin).orElse(null))
                .and(Optional.ofNullable(mileageMax).map(CarSpecifications::mileageMax).orElse(null))
                .and(Optional.ofNullable(location).map(CarSpecifications::location).orElse(null))
                .and(Optional.ofNullable(isVip).map(CarSpecifications::vip).orElse(null))
                .and(CarSpecifications.vipNotExpired());

        Sort sortConfig;
        if ("price_asc".equalsIgnoreCase(sort)) {
            sortConfig = Sort.by("price").ascending();
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            sortConfig = Sort.by("price").descending();
        } else {
            sortConfig = Sort.by("createdAt").descending();
        }

        if (all) {
            List<CarResponse> cars = carRepository.findAll(spec, sortConfig)
                    .stream()
                    .map(CarMapper::toResponse)
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(
                    cars,
                    PageRequest.of(0, cars.size() == 0 ? 1 : cars.size(), sortConfig),
                    cars.size()
            );
        }

        return carRepository.findAll(spec, PageRequest.of(page, size, sortConfig)).map(CarMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CarResponse> listAll() {
        Specification<CarListing> spec = Specification.where(CarSpecifications.active())
                .and(CarSpecifications.vipNotExpired());
        return carRepository.findAll(spec, Sort.by("createdAt").descending())
                .stream()
                .map(CarMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CarResponse get(Long id) {
        CarListing car = carRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Car not found"));
        return CarMapper.toResponse(car);
    }

    @Transactional
    public CarResponse create(Long ownerId, CarRequest request) {
        User owner = userRepository.findById(ownerId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        CarListing car = CarMapper.fromRequest(request);
        car.setOwner(owner);
        carRepository.save(car);
        return CarMapper.toResponse(car);
    }

    @Transactional
    public CarResponse update(Long id, Long ownerId, boolean isAdmin, CarRequest request) {
        CarListing car = carRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Car not found"));
        if (!isAdmin && !car.getOwner().getId().equals(ownerId)) {
            throw new SecurityException("Not allowed");
        }
        CarMapper.updateEntity(car, request);
        carRepository.save(car);
        return CarMapper.toResponse(car);
    }

    @Transactional
    public void delete(Long id, Long ownerId, boolean isAdmin) {
        CarListing car = carRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Car not found"));
        if (!isAdmin && !car.getOwner().getId().equals(ownerId)) {
            throw new SecurityException("Not allowed");
        }
        car.setActive(false);
        carRepository.save(car);
    }

    @Transactional
    public CarResponse markVip(Long id, Long ownerId, boolean isAdmin, Integer days) {
        CarListing car = carRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Car not found"));
        if (!isAdmin && !car.getOwner().getId().equals(ownerId)) {
            throw new SecurityException("Not allowed");
        }
        int vipDays = days != null ? days : vipDefaultDays;
        car.setVip(true);
        car.setVipExpiresAt(Instant.now().plus(vipDays, ChronoUnit.DAYS));
        carRepository.save(car);
        return CarMapper.toResponse(car);
    }

    @Transactional(readOnly = true)
    public List<CarResponse> listByOwner(Long ownerId) {
        return carRepository.findAll((root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId))
                .stream()
                .map(CarMapper::toResponse)
                .toList();
    }
}

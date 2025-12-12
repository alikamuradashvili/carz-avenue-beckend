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

    public Page<CarResponse> list(Optional<String> make,
                                  Optional<String> model,
                                  Optional<Integer> yearMin,
                                  Optional<Integer> yearMax,
                                  Optional<Double> priceMin,
                                  Optional<Double> priceMax,
                                  Optional<String> fuelType,
                                  Optional<String> transmission,
                                  Optional<String> bodyType,
                                  Optional<Integer> mileageMin,
                                  Optional<Integer> mileageMax,
                                  Optional<String> location,
                                  Optional<Boolean> isVip,
                                  int page,
                                  int size,
                                  String sort) {
        Specification<CarListing> spec = Specification.where(CarSpecifications.active())
                .and(make.map(CarSpecifications::make).orElse(null))
                .and(model.map(CarSpecifications::model).orElse(null))
                .and(yearMin.map(CarSpecifications::yearMin).orElse(null))
                .and(yearMax.map(CarSpecifications::yearMax).orElse(null))
                .and(priceMin.map(CarSpecifications::priceMin).orElse(null))
                .and(priceMax.map(CarSpecifications::priceMax).orElse(null))
                .and(fuelType.map(CarSpecifications::fuelType).orElse(null))
                .and(transmission.map(CarSpecifications::transmission).orElse(null))
                .and(bodyType.map(CarSpecifications::bodyType).orElse(null))
                .and(mileageMin.map(CarSpecifications::mileageMin).orElse(null))
                .and(mileageMax.map(CarSpecifications::mileageMax).orElse(null))
                .and(location.map(CarSpecifications::location).orElse(null))
                .and(isVip.map(CarSpecifications::vip).orElse(null))
                .and(CarSpecifications.vipNotExpired());

        Sort sortConfig;
        if ("price_asc".equalsIgnoreCase(sort)) {
            sortConfig = Sort.by("price").ascending();
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            sortConfig = Sort.by("price").descending();
        } else {
            sortConfig = Sort.by("createdAt").descending();
        }
        return carRepository.findAll(spec, PageRequest.of(page, size, sortConfig))
                .map(CarMapper::toResponse);
    }

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

    public List<CarResponse> listByOwner(Long ownerId) {
        return carRepository.findAll((root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId))
                .stream()
                .map(CarMapper::toResponse)
                .toList();
    }
}

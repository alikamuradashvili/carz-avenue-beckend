package com.carzavenue.backend.admin;

import com.carzavenue.backend.admin.dto.AdminListingRequest;
import com.carzavenue.backend.admin.dto.AdminListingResponse;
import com.carzavenue.backend.admin.dto.AdminUserResponse;
import com.carzavenue.backend.admin.dto.AdminListingFiltersResponse;
import com.carzavenue.backend.admin.dto.AdminUserOption;
import com.carzavenue.backend.car.AdStatus;
import com.carzavenue.backend.car.CarListing;
import com.carzavenue.backend.car.CarListingRepository;
import com.carzavenue.backend.car.CarMapper;
import com.carzavenue.backend.car.CarService;
import com.carzavenue.backend.car.dto.CarRequest;
import com.carzavenue.backend.car.dto.CarResponse;
import com.carzavenue.backend.user.Role;
import com.carzavenue.backend.user.User;
import com.carzavenue.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {
    private final CarListingRepository carListingRepository;
    private final UserRepository userRepository;
    private final CarService carService;

    public AdminService(CarListingRepository carListingRepository,
                        UserRepository userRepository,
                        CarService carService) {
        this.carListingRepository = carListingRepository;
        this.userRepository = userRepository;
        this.carService = carService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(int page, int size, String q) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users;
        if (q == null || q.isBlank()) {
            users = userRepository.findAll(pageable);
        } else {
            String query = q.trim();
            users = userRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(query, query, pageable);
        }
        return users.map(this::toAdminUser);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        return toAdminUser(user);
    }

    @Transactional
    public AdminUserResponse updateRole(Long id, Role role, Role actorRole) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        requireAdministrator(actorRole);
        if (user.getRole() == Role.ADMIN && role == Role.USER) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot demote the last admin");
            }
        }
        if (user.getRole() == Role.ADMINISTRATOR && role != Role.ADMINISTRATOR) {
            long adminCount = userRepository.countByRole(Role.ADMINISTRATOR);
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot demote the last administrator");
            }
        }
        user.setRole(role);
        userRepository.save(user);
        return toAdminUser(user);
    }

    @Transactional
    public AdminUserResponse updateStatus(Long id, boolean enabled, Role actorRole) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (user.getRole() == Role.ADMINISTRATOR) {
            requireAdministrator(actorRole);
            long adminCount = userRepository.countByRole(Role.ADMINISTRATOR);
            if (!enabled && adminCount <= 1) {
                throw new IllegalArgumentException("Cannot disable the last administrator");
            }
        }
        if (user.getRole() == Role.ADMIN) {
            requireAdministrator(actorRole);
            if (!enabled) {
                long adminCount = userRepository.countByRole(Role.ADMIN);
                if (adminCount <= 1) {
                    throw new IllegalArgumentException("Cannot disable the last admin");
                }
            }
        }
        if (user.getRole() == Role.USER && !enabled) {
            requireAdministratorOrAdmin(actorRole);
        }
        user.setBlocked(!enabled);
        userRepository.save(user);
        return toAdminUser(user);
    }

    @Transactional(readOnly = true)
    public Page<AdminListingResponse> listAds(int page,
                                              int size,
                                              AdStatus status,
                                              String q,
                                              String listingType,
                                              String make,
                                              String model,
                                              String ownerEmail,
                                              String ownerName,
                                              String fuelType,
                                              String transmission,
                                              String bodyType,
                                              String color,
                                              Double engineVolume,
                                              Integer mileage,
                                              String vinCode,
                                              Integer yearMin,
                                              Integer yearMax,
                                              Double priceMin,
                                              Double priceMax,
                                              Boolean active) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Specification<CarListing> spec = buildListingSpec(status, q, listingType, make, model, ownerEmail, ownerName,
                fuelType, transmission, bodyType, color, engineVolume, mileage, vinCode,
                yearMin, yearMax, priceMin, priceMax, active);
        return carListingRepository.findAll(spec, pageable).map(this::toAdminListing);
    }

    @Transactional(readOnly = true)
    public AdminListingResponse getAd(Long id) {
        CarListing car = carListingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Listing not found"));
        return toAdminListing(car);
    }

    @Transactional
    public CarResponse createAd(AdminListingRequest request) {
        return carService.create(request.getOwnerId(), request);
    }

    @Transactional
    public CarResponse createAdWithImages(Long ownerId, CarRequest request,
                                          org.springframework.web.multipart.MultipartFile[] images) throws java.io.IOException {
        return carService.createWithImages(ownerId, request, images);
    }

    @Transactional
    public CarResponse updateAd(Long id, CarRequest request) {
        CarListing car = carListingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Listing not found"));
        return carService.update(id, car.getOwner().getId(), true, request);
    }

    @Transactional
    public void deleteAd(Long id) {
        CarListing car = carListingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Listing not found"));
        carService.delete(id, car.getOwner().getId(), true);
    }

    @Transactional
    public AdminListingResponse updateAdStatus(Long id, AdStatus status) {
        CarListing car = carListingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Listing not found"));
        car.setStatus(status);
        car.setActive(status == AdStatus.ACTIVE);
        carListingRepository.save(car);
        return toAdminListing(car);
    }

    @Transactional(readOnly = true)
    public List<CarResponse> listAllListings() {
        return carListingRepository.findAll().stream()
                .map(CarMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteListing(Long id) {
        carListingRepository.deleteById(id);
    }

    private AdminUserResponse toAdminUser(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .blocked(user.isBlocked())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private AdminListingResponse toAdminListing(CarListing car) {
        return AdminListingResponse.builder()
                .id(car.getId())
                .ownerId(car.getOwner().getId())
                .ownerEmail(car.getOwner().getEmail())
                .title(car.getTitle())
                .listingType(car.getListingType())
                .make(car.getMake())
                .model(car.getModel())
                .year(car.getYear())
                .price(car.getPrice())
                .location(car.getLocation())
                .status(car.getStatus())
                .active(car.isActive())
                .createdAt(car.getCreatedAt())
                .updatedAt(car.getUpdatedAt())
                .build();
    }

    private void requireAdministrator(Role actorRole) {
        if (actorRole != Role.ADMINISTRATOR) {
            throw new IllegalArgumentException("Administrator privileges required");
        }
    }

    private void requireAdministratorOrAdmin(Role actorRole) {
        if (actorRole != Role.ADMINISTRATOR && actorRole != Role.ADMIN) {
            throw new IllegalArgumentException("Admin privileges required");
        }
    }

    private Specification<CarListing> buildListingSpec(AdStatus status,
                                                       String q,
                                                       String listingType,
                                                       String make,
                                                       String model,
                                                       String ownerEmail,
                                                       String ownerName,
                                                       String fuelType,
                                                       String transmission,
                                                       String bodyType,
                                                       String color,
                                                       Double engineVolume,
                                                       Integer mileage,
                                                       String vinCode,
                                                       Integer yearMin,
                                                       Integer yearMax,
                                                       Double priceMin,
                                                       Double priceMax,
                                                       Boolean active) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("isActive"), active));
            }
            if (listingType != null && !listingType.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("listingType")), listingType.trim().toLowerCase()));
            }
            if (make != null && !make.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("make")), "%" + make.trim().toLowerCase() + "%"));
            }
            if (model != null && !model.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("model")), "%" + model.trim().toLowerCase() + "%"));
            }
            if (ownerEmail != null && !ownerEmail.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("owner").get("email")), "%" + ownerEmail.trim().toLowerCase() + "%"));
            }
            if (ownerName != null && !ownerName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("owner").get("name")), "%" + ownerName.trim().toLowerCase() + "%"));
            }
            if (fuelType != null && !fuelType.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("fuelType")), fuelType.trim().toLowerCase()));
            }
            if (transmission != null && !transmission.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("transmission")), transmission.trim().toLowerCase()));
            }
            if (bodyType != null && !bodyType.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("bodyType")), bodyType.trim().toLowerCase()));
            }
            if (color != null && !color.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("color")), color.trim().toLowerCase()));
            }
            if (engineVolume != null) {
                predicates.add(cb.equal(root.get("engineVolume"), engineVolume));
            }
            if (mileage != null) {
                predicates.add(cb.equal(root.get("mileage"), mileage));
            }
            if (vinCode != null && !vinCode.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("vinCode")), vinCode.trim().toUpperCase()));
            }
            if (yearMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("year"), yearMin));
            }
            if (yearMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("year"), yearMax));
            }
            if (priceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceMin));
            }
            if (priceMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceMax));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                var ownerEmailPath = cb.lower(root.get("owner").get("email"));
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("make")), like),
                        cb.like(cb.lower(root.get("model")), like),
                        cb.like(cb.lower(root.get("location")), like),
                        cb.like(ownerEmailPath, like)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public AdminListingFiltersResponse listingFilters() {
        return AdminListingFiltersResponse.builder()
                .makes(carListingRepository.findDistinctMakes())
                .models(carListingRepository.findDistinctModels())
                .listingTypes(carListingRepository.findDistinctListingTypes())
                .fuelTypes(carListingRepository.findDistinctFuelTypes())
                .transmissions(carListingRepository.findDistinctTransmissions())
                .bodyTypes(carListingRepository.findDistinctBodyTypes())
                .colors(carListingRepository.findDistinctColors())
                .ownerEmails(carListingRepository.findDistinctOwnerEmails())
                .ownerNames(carListingRepository.findDistinctOwnerNames())
                .years(carListingRepository.findDistinctYears())
                .engineVolumes(carListingRepository.findDistinctEngineVolumes())
                .mileages(carListingRepository.findDistinctMileages())
                .minPrice(carListingRepository.findMinPrice())
                .maxPrice(carListingRepository.findMaxPrice())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminUserOption> userOptions() {
        return userRepository.findAllByOrderByEmailAsc().stream()
                .map(user -> AdminUserOption.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .build())
                .toList();
    }
}

package com.carzavenue.backend.car;

import com.carzavenue.backend.car.dto.CarRequest;
import com.carzavenue.backend.car.dto.CarResponse;
import com.carzavenue.backend.image.ImageEntity;
import com.carzavenue.backend.image.ImageStorageService;
import com.carzavenue.backend.payment.AccountService;
import com.carzavenue.backend.user.User;
import com.carzavenue.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CarService {
    private final CarListingRepository carRepository;
    private final CarManufacturerRepository manufacturerRepository;
    private final CarModelRepository modelRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final AccountService accountService;
    private static final BigDecimal QUICK_FILTER_PRICE = new BigDecimal("1.00");
    private final int vipDefaultDays;

    public CarService(CarListingRepository carRepository,
                      CarManufacturerRepository manufacturerRepository,
                      CarModelRepository modelRepository,
                      UserRepository userRepository,
                      ImageStorageService imageStorageService,
                      AccountService accountService,
                      @org.springframework.beans.factory.annotation.Value("${app.vip.default-days:7}") int vipDefaultDays) {
        this.carRepository = carRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.modelRepository = modelRepository;
        this.userRepository = userRepository;
        this.imageStorageService = imageStorageService;
        this.accountService = accountService;
        this.vipDefaultDays = vipDefaultDays;
    }

    @Transactional(readOnly = true)
    public Page<CarResponse> list(String make,
                                  String model,
                                  java.util.List<PackageType> packageTypes,
                                  java.util.List<VehicleCategory> categories,
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
                .and(CarSpecifications.packageTypeIn(packageTypes))
                .and(CarSpecifications.categoryIn(categories))
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
        request.setVinCode(normalizeVinCode(resolveVinCode(request)));
        if (request.getContactPhone() == null || request.getContactPhone().trim().isEmpty()) {
            request.setContactPhone(owner.getPhoneNumber());
        }
        ensureManufacturerModelExists(request.getMake(), request.getModel());
        CarListing car = CarMapper.fromRequest(request);
        car.setTitle(buildTitle(request));
        car.setOwner(owner);
        carRepository.save(car);
        chargeQuickFilterIfNeeded(ownerId, request.getPackageTypes(), request.getPackageType(), car.getId());
        return CarMapper.toResponse(car);
    }

    @Transactional
    public CarResponse createWithImages(Long ownerId, CarRequest request,
                                        org.springframework.web.multipart.MultipartFile[] images) throws java.io.IOException {
        User owner = userRepository.findById(ownerId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        request.setVinCode(normalizeVinCode(resolveVinCode(request)));
        if (request.getContactPhone() == null || request.getContactPhone().trim().isEmpty()) {
            request.setContactPhone(owner.getPhoneNumber());
        }
        ensureManufacturerModelExists(request.getMake(), request.getModel());
        CarListing car = CarMapper.fromRequest(request);
        car.setTitle(buildTitle(request));
        car.setOwner(owner);

        List<String> photoUrls = new ArrayList<>();
        if (images != null) {
            for (org.springframework.web.multipart.MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    ImageEntity saved = imageStorageService.save(image);
                    photoUrls.add("/images/" + saved.getId());
                    if (car.getImageId() == null) {
                        car.setImageId(saved.getId());
                    }
                }
            }
        }
        if (!photoUrls.isEmpty()) {
            car.setPhotos(photoUrls);
        }

        carRepository.save(car);
        chargeQuickFilterIfNeeded(ownerId, request.getPackageTypes(), request.getPackageType(), car.getId());
        return CarMapper.toResponse(car);
    }

    @Transactional
    public CarResponse update(Long id, Long ownerId, boolean isAdmin, CarRequest request) {
        CarListing car = carRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Car not found"));
        if (!isAdmin && !car.getOwner().getId().equals(ownerId)) {
            throw new SecurityException("Not allowed");
        }
        request.setVinCode(normalizeVinCode(resolveVinCode(request)));
        ensureManufacturerModelExists(request.getMake(), request.getModel());
        CarMapper.updateEntity(car, request);
        car.setTitle(buildTitle(request));
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
        car.setStatus(AdStatus.INACTIVE);
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

    @Transactional(readOnly = true)
    public List<String> listModelsByManufacturer(String manufacturer) {
        if (manufacturer == null || manufacturer.trim().isEmpty()) {
            throw new IllegalArgumentException("manufacturer is required");
        }
        String cleanManufacturer = manufacturer.trim();
        Optional<CarManufacturer> carManufacturer = manufacturerRepository.findByNameIgnoreCase(cleanManufacturer);
        if (carManufacturer.isEmpty()) {
            return List.of();
        }
        return modelRepository.findAllByManufacturerIdOrderByNameAsc(carManufacturer.get().getId())
                .stream()
                .map(CarModel::getName)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CarManufacturer> listManufacturers() {
        return manufacturerRepository.findAllByOrderByIdAsc();
    }

    private void ensureManufacturerModelExists(String manufacturer, String model) {
        String cleanManufacturer = requireText(manufacturer, "manufacturer");
        String cleanModel = requireText(model, "model");
        CarManufacturer carManufacturer = manufacturerRepository.findByNameIgnoreCase(cleanManufacturer)
                .orElseGet(() -> {
                    try {
                        return manufacturerRepository.save(CarManufacturer.builder()
                                .name(cleanManufacturer)
                                .build());
                    } catch (DataIntegrityViolationException ex) {
                        return manufacturerRepository.findByNameIgnoreCase(cleanManufacturer)
                                .orElseThrow(() -> ex);
                    }
                });
        modelRepository.findByManufacturerIdAndNameIgnoreCase(carManufacturer.getId(), cleanModel)
                .orElseGet(() -> {
                    try {
                        return modelRepository.save(CarModel.builder()
                                .manufacturer(carManufacturer)
                                .name(cleanModel)
                                .build());
                    } catch (DataIntegrityViolationException ex) {
                        return modelRepository.findByManufacturerIdAndNameIgnoreCase(carManufacturer.getId(), cleanModel)
                                .orElseThrow(() -> ex);
                    }
                });
    }

    private String resolveVinCode(CarRequest request) {
        String vinCode = request.getVinCode();
        if (vinCode != null && !vinCode.trim().isEmpty()) {
            return vinCode;
        }
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return vinCode;
        }
        HttpServletRequest httpRequest = attrs.getRequest();
        if (httpRequest == null) {
            return vinCode;
        }
        String paramVinCode = httpRequest.getParameter("vinCode");
        if (paramVinCode != null && !paramVinCode.trim().isEmpty()) {
            return paramVinCode;
        }
        return vinCode;
    }

    private String normalizeVinCode(String vinCode) {
        if (vinCode == null || vinCode.trim().isEmpty()) {
            return null;
        }
        String normalized = vinCode.replaceAll("\\s+", "").toUpperCase();
        if (normalized.length() != 17) {
            throw new IllegalArgumentException("vinCode must be 17 characters");
        }
        if (!normalized.matches("[A-HJ-NPR-Z0-9]{17}")) {
            throw new IllegalArgumentException("vinCode contains invalid characters");
        }
        return normalized;
    }

    private void chargeQuickFilterIfNeeded(Long ownerId, List<PackageType> packageTypes, PackageType fallback, Long listingId) {
        List<PackageType> resolved = resolvePackageTypes(packageTypes, fallback);
        if (resolved.isEmpty()) {
            return;
        }
        String currency = accountService.normalizeCurrency(null);
        String referenceId = listingId != null ? String.valueOf(listingId) : "package";
        BigDecimal total = QUICK_FILTER_PRICE.multiply(new BigDecimal(resolved.size()));
        String idempotencyKey = buildPackageIdempotencyKey(referenceId, resolved);
        accountService.chargePackage(ownerId, currency, total, "listing", referenceId, idempotencyKey);
    }

    private List<PackageType> resolvePackageTypes(List<PackageType> packageTypes, PackageType fallback) {
        if (packageTypes != null) {
            List<PackageType> normalized = packageTypes.stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }
        if (fallback != null) {
            return List.of(fallback);
        }
        return List.of();
    }

    private String buildPackageIdempotencyKey(String referenceId, List<PackageType> resolved) {
        String joined = resolved.stream()
                .map(Enum::name)
                .sorted()
                .reduce((a, b) -> a + "." + b)
                .orElse("packages");
        int hash = joined.hashCode();
        String base = referenceId + ":packages:" + resolved.size() + ":" + Math.abs(hash);
        return base.length() > 64 ? base.substring(0, 64) : base;
    }

    private String buildTitle(CarRequest request) {
        String make = requireText(request.getMake(), "manufacturer");
        String model = requireText(request.getModel(), "model");
        Integer year = request.getYear();
        if (year == null) {
            throw new IllegalArgumentException("year is required");
        }
        String listingType = requireText(request.getListingType(), "listingType");
        return make + " " + model + " " + year + " \u2013 " + listingType;
    }

    private String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}

package com.carzavenue.backend.admin;

import com.carzavenue.backend.admin.dto.AdminListingRequest;
import com.carzavenue.backend.admin.dto.AdminListingResponse;
import com.carzavenue.backend.admin.dto.AdminUserResponse;
import com.carzavenue.backend.admin.dto.AdminUserAccountResponse;
import com.carzavenue.backend.admin.dto.AdminResetPasswordResponse;
import com.carzavenue.backend.admin.dto.AdminPaymentConfigRequest;
import com.carzavenue.backend.admin.dto.AdminPaymentConfigResponse;
import com.carzavenue.backend.admin.dto.AdminListingFiltersResponse;
import com.carzavenue.backend.admin.dto.AdminUserOption;
import com.carzavenue.backend.common.PageResponse;
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
import com.carzavenue.backend.payment.AccountRepository;
import com.carzavenue.backend.payment.Account;
import com.carzavenue.backend.payment.LedgerEntryRepository;
import com.carzavenue.backend.payment.LedgerEntry;
import com.carzavenue.backend.payment.LedgerType;
import com.carzavenue.backend.payment.dto.LedgerEntryResponse;
import com.carzavenue.backend.payment.PaymentConfigService;
import com.carzavenue.backend.payment.PaymentConfig;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.List;
import java.time.Instant;

@Service
public class AdminService {
    private final CarListingRepository carListingRepository;
    private final UserRepository userRepository;
    private final CarService carService;
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaymentConfigService paymentConfigService;

    public AdminService(CarListingRepository carListingRepository,
                        UserRepository userRepository,
                        CarService carService,
                        AccountRepository accountRepository,
                        LedgerEntryRepository ledgerEntryRepository,
                        PasswordEncoder passwordEncoder,
                        PaymentConfigService paymentConfigService) {
        this.carListingRepository = carListingRepository;
        this.userRepository = userRepository;
        this.carService = carService;
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.passwordEncoder = passwordEncoder;
        this.paymentConfigService = paymentConfigService;
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

    @Transactional(readOnly = true)
    public List<AdminUserAccountResponse> getUserAccounts(Long id) {
        userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        return accountRepository.findByUserId(id).stream()
                .map(this::toAdminAccount)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<LedgerEntryResponse> getUserLedger(Long id,
                                                           String currency,
                                                           LedgerType type,
                                                           Instant from,
                                                           Instant to,
                                                           Pageable pageable) {
        userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        List<Account> accounts;
        if (currency != null && !currency.isBlank()) {
            accounts = accountRepository.findByUserId(id).stream()
                    .filter(account -> currency.trim().equalsIgnoreCase(account.getCurrency()))
                    .toList();
        } else {
            accounts = accountRepository.findByUserId(id);
        }
        if (accounts.isEmpty()) {
            return PageResponse.from(Page.empty(pageable));
        }
        List<Long> accountIds = accounts.stream().map(Account::getId).toList();
        Page<LedgerEntry> page;
        boolean hasType = type != null;
        boolean hasRange = from != null && to != null;
        if (hasType && hasRange) {
            page = ledgerEntryRepository.findByAccountIdInAndTypeAndCreatedAtBetween(accountIds, type, from, to, pageable);
        } else if (hasType) {
            page = ledgerEntryRepository.findByAccountIdInAndType(accountIds, type, pageable);
        } else if (hasRange) {
            page = ledgerEntryRepository.findByAccountIdInAndCreatedAtBetween(accountIds, from, to, pageable);
        } else {
            page = ledgerEntryRepository.findByAccountIdIn(accountIds, pageable);
        }
        return PageResponse.from(page.map(this::toLedgerResponse));
    }

    @Transactional
    public AdminResetPasswordResponse resetPassword(Long id, Role actorRole) {
        requireAdministratorOrAdmin(actorRole);
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        return AdminResetPasswordResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .tempPassword(tempPassword)
                .build();
    }

    @Transactional
    public void setPassword(Long id, String newPassword, Role actorRole) {
        requireAdministratorOrAdmin(actorRole);
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AdminPaymentConfigResponse getPaymentConfig() {
        PaymentConfig config = paymentConfigService.getOrCreate();
        return toPaymentConfigResponse(config);
    }

    @Transactional
    public AdminPaymentConfigResponse updatePaymentConfig(AdminPaymentConfigRequest request, Role actorRole) {
        requireAdministrator(actorRole);
        PaymentConfig incoming = PaymentConfig.builder()
                .apiUrl(request.getApiUrl())
                .testKey(request.getTestKey())
                .liveKey(request.getLiveKey())
                .mode(request.getMode())
                .build();
        PaymentConfig updated = paymentConfigService.update(incoming);
        return toPaymentConfigResponse(updated);
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
    public PageResponse<AdminListingResponse> listAds(int page,
                                                      int size,
                                                      String sort,
                                                      AdStatus status,
                                                      String q,
                                                      String makeId,
                                                      String modelId,
                                                      String locationId,
                                                      Double priceMin,
                                                      Double priceMax,
                                                      java.time.LocalDateTime createdFrom,
                                                      java.time.LocalDateTime createdTo,
                                                      Long sellerId,
                                                      String sellerEmail) {
        Sort resolvedSort = resolveSort(sort);
        Pageable pageable = PageRequest.of(page, size, resolvedSort);
        java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
        java.time.Instant createdFromInstant = createdFrom == null ? null : createdFrom.atZone(zoneId).toInstant();
        java.time.Instant createdToInstant = createdTo == null ? null : createdTo.atZone(zoneId).toInstant();
        Specification<CarListing> spec = buildListingSpec(status, q, makeId, modelId, locationId,
                priceMin, priceMax, createdFromInstant, createdToInstant, sellerId, sellerEmail);
        return PageResponse.from(carListingRepository.findAll(spec, pageable).map(this::toAdminListing));
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

    @Transactional
    public AdminListingResponse updateAdPackageType(Long id, com.carzavenue.backend.car.PackageType packageType) {
        CarListing car = carListingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Listing not found"));
        car.setPackageType(packageType);
        carListingRepository.save(car);
        return toAdminListing(car);
    }

    @Transactional
    public AdminListingResponse updateAdCategory(Long id, com.carzavenue.backend.car.VehicleCategory category) {
        CarListing car = carListingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Listing not found"));
        car.setCategory(category);
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
                .phoneNumber(user.getPhoneNumber())
                .provider(user.getProvider())
                .role(user.getRole())
                .blocked(user.isBlocked())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private AdminUserAccountResponse toAdminAccount(Account account) {
        return AdminUserAccountResponse.builder()
                .id(account.getId())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .availableBalance(account.getAvailableBalance())
                .holdBalance(account.getHoldBalance())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private LedgerEntryResponse toLedgerResponse(LedgerEntry entry) {
        return LedgerEntryResponse.builder()
                .id(entry.getId())
                .accountId(entry.getAccount() != null ? entry.getAccount().getId() : null)
                .direction(entry.getDirection())
                .amount(entry.getAmount())
                .type(entry.getType())
                .referenceType(entry.getReferenceType())
                .referenceId(entry.getReferenceId())
                .status(entry.getStatus())
                .idempotencyKey(entry.getIdempotencyKey())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private AdminPaymentConfigResponse toPaymentConfigResponse(PaymentConfig config) {
        return AdminPaymentConfigResponse.builder()
                .id(config.getId())
                .apiUrl(config.getApiUrl())
                .testKey(config.getTestKey())
                .liveKey(config.getLiveKey())
                .mode(config.getMode())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private AdminListingResponse toAdminListing(CarListing car) {
        return AdminListingResponse.builder()
                .id(car.getId())
                .ownerId(car.getOwner().getId())
                .ownerEmail(car.getOwner().getEmail())
                .title(car.getTitle())
                .listingType(car.getListingType())
                .packageType(car.getPackageType())
                .category(car.getCategory())
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
                                                       String makeId,
                                                       String modelId,
                                                       String locationId,
                                                       Double priceMin,
                                                       Double priceMax,
                                                       java.time.Instant createdFrom,
                                                       java.time.Instant createdTo,
                                                       Long sellerId,
                                                       String sellerEmail) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (makeId != null && !makeId.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("make")), "%" + makeId.trim().toLowerCase() + "%"));
            }
            if (modelId != null && !modelId.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("model")), "%" + modelId.trim().toLowerCase() + "%"));
            }
            if (locationId != null && !locationId.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + locationId.trim().toLowerCase() + "%"));
            }
            if (sellerId != null) {
                predicates.add(cb.equal(root.get("owner").get("id"), sellerId));
            }
            if (sellerEmail != null && !sellerEmail.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("owner").get("email")), "%" + sellerEmail.trim().toLowerCase() + "%"));
            }
            if (priceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceMin));
            }
            if (priceMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceMax));
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
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

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Order.desc("createdAt"));
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (parts.length < 2) {
            return Sort.by(Sort.Order.asc(field));
        }
        String direction = parts[1].trim();
        return "desc".equalsIgnoreCase(direction)
                ? Sort.by(Sort.Order.desc(field))
                : Sort.by(Sort.Order.asc(field));
    }

    @Transactional(readOnly = true)
    public AdminListingFiltersResponse listingFilters() {
        return AdminListingFiltersResponse.builder()
                .makes(carListingRepository.findDistinctMakes())
                .models(carListingRepository.findDistinctModels())
                .listingTypes(carListingRepository.findDistinctListingTypes())
                .packageTypes(carListingRepository.findDistinctPackageTypes())
                .categories(carListingRepository.findDistinctCategories())
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

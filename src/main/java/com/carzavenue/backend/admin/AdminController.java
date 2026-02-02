package com.carzavenue.backend.admin;

import com.carzavenue.backend.admin.dto.AdminListingRequest;
import com.carzavenue.backend.admin.dto.AdminListingResponse;
import com.carzavenue.backend.admin.dto.AdminListingStatusRequest;
import com.carzavenue.backend.admin.dto.AdminCarMultipartRequest;
import com.carzavenue.backend.admin.dto.AdminListingFiltersResponse;
import com.carzavenue.backend.admin.dto.AdminUserOption;
import com.carzavenue.backend.admin.dto.AdminUserResponse;
import com.carzavenue.backend.admin.dto.AdminUserRoleRequest;
import com.carzavenue.backend.admin.dto.AdminUserStatusRequest;
import com.carzavenue.backend.car.AdStatus;
import com.carzavenue.backend.car.dto.CarRequest;
import com.carzavenue.backend.car.dto.CarResponse;
import com.carzavenue.backend.common.ApiResponse;
import com.carzavenue.backend.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import com.carzavenue.backend.security.SecurityUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN','ADMINISTRATOR')")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> users(@RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size,
                                                                      @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listUsers(page, size, q)));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> user(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getUser(id)));
    }

    @GetMapping("/users/options")
    public ResponseEntity<ApiResponse<List<AdminUserOption>>> userOptions() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.userOptions()));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateRole(@PathVariable Long id,
                                                                     @Validated @RequestBody AdminUserRoleRequest request,
                                                                     @org.springframework.security.core.annotation.AuthenticationPrincipal SecurityUser principal) {
        var actorRole = principal == null ? null : principal.getUser().getRole();
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateRole(id, request.getRole(), actorRole)));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateStatus(@PathVariable Long id,
                                                                       @Validated @RequestBody AdminUserStatusRequest request,
                                                                       @org.springframework.security.core.annotation.AuthenticationPrincipal SecurityUser principal) {
        var actorRole = principal == null ? null : principal.getUser().getRole();
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateStatus(id, request.getEnabled(), actorRole)));
    }

    @GetMapping("/ads")
    public ResponseEntity<ApiResponse<PageResponse<AdminListingResponse>>> ads(@RequestParam(defaultValue = "0") int page,
                                                                               @RequestParam(defaultValue = "20") int size,
                                                                               @RequestParam(required = false) String sort,
                                                                               @RequestParam(required = false) AdStatus status,
                                                                               @RequestParam(required = false) String q,
                                                                               @RequestParam(required = false) String makeId,
                                                                               @RequestParam(required = false) String modelId,
                                                                               @RequestParam(required = false) String locationId,
                                                                               @RequestParam(required = false) Double priceMin,
                                                                               @RequestParam(required = false) Double priceMax,
                                                                               @RequestParam(required = false)
                                                                               @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
                                                                               java.time.LocalDateTime createdFrom,
                                                                               @RequestParam(required = false)
                                                                               @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
                                                                               java.time.LocalDateTime createdTo,
                                                                               @RequestParam(required = false) Long sellerId,
                                                                               @RequestParam(required = false) String sellerEmail) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.listAds(page, size, sort, status, q, makeId, modelId, locationId,
                        priceMin, priceMax, createdFrom, createdTo, sellerId, sellerEmail)));
    }

    @GetMapping("/ads/filters")
    public ResponseEntity<ApiResponse<AdminListingFiltersResponse>> listingFilters() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listingFilters()));
    }

    @GetMapping("/ads/{id}")
    public ResponseEntity<ApiResponse<AdminListingResponse>> ad(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAd(id)));
    }

    @PostMapping("/ads")
    public ResponseEntity<ApiResponse<CarResponse>> createAd(@Validated @RequestBody AdminListingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.createAd(request)));
    }

    @PostMapping(value = "/ads/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CarResponse>> createAdMultipart(@Validated @ModelAttribute AdminCarMultipartRequest request,
                                                                      @RequestPart(value = "images", required = false)
                                                                      org.springframework.web.multipart.MultipartFile[] images) throws java.io.IOException {
        CarRequest mappedRequest = new CarRequest();
        mappedRequest.setTitle(request.getTitle());
        mappedRequest.setMake(request.getMake());
        mappedRequest.setModel(request.getModel());
        mappedRequest.setYear(request.getYear());
        mappedRequest.setMileage(request.getMileage());
        mappedRequest.setFuelType(request.getFuelType());
        mappedRequest.setTransmission(request.getTransmission());
        mappedRequest.setBodyType(request.getBodyType());
        mappedRequest.setEngineVolume(request.getEngineVolume());
        mappedRequest.setPrice(request.getPrice());
        mappedRequest.setListingType(request.getListingType());
        mappedRequest.setColor(request.getColor());
        mappedRequest.setDescription(request.getDescription());
        mappedRequest.setLocation(request.getLocation());
        mappedRequest.setImageId(request.getImageId());
        mappedRequest.setVinCode(request.getVinCode());

        return ResponseEntity.ok(ApiResponse.ok(
                adminService.createAdWithImages(request.getOwnerId(), mappedRequest, images)));
    }

    @PutMapping("/ads/{id}")
    public ResponseEntity<ApiResponse<CarResponse>> updateAd(@PathVariable Long id,
                                                             @Validated @RequestBody CarRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateAd(id, request)));
    }

    @DeleteMapping("/ads/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAd(@PathVariable Long id) {
        adminService.deleteAd(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PatchMapping("/ads/{id}/status")
    public ResponseEntity<ApiResponse<AdminListingResponse>> updateAdStatus(@PathVariable Long id,
                                                                            @Validated @RequestBody AdminListingStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateAdStatus(id, request.getStatus())));
    }

    // Legacy endpoints
    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<List<CarResponse>>> allListings() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listAllListings()));
    }

    @DeleteMapping("/listings/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteListing(@PathVariable Long id) {
        adminService.deleteListing(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}

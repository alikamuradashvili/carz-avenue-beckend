package com.carzavenue.backend.admin;

import com.carzavenue.backend.admin.dto.AdminListingRequest;
import com.carzavenue.backend.admin.dto.AdminListingResponse;
import com.carzavenue.backend.admin.dto.AdminListingStatusRequest;
import com.carzavenue.backend.admin.dto.AdminUserResponse;
import com.carzavenue.backend.admin.dto.AdminUserRoleRequest;
import com.carzavenue.backend.admin.dto.AdminUserStatusRequest;
import com.carzavenue.backend.car.AdStatus;
import com.carzavenue.backend.car.dto.CarRequest;
import com.carzavenue.backend.car.dto.CarResponse;
import com.carzavenue.backend.common.ApiResponse;
import com.carzavenue.backend.security.SecurityUser;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<ApiResponse<Page<AdminListingResponse>>> ads(@RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size,
                                                                       @RequestParam(required = false) AdStatus status,
                                                                       @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listAds(page, size, status, q)));
    }

    @GetMapping("/ads/{id}")
    public ResponseEntity<ApiResponse<AdminListingResponse>> ad(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAd(id)));
    }

    @PostMapping("/ads")
    public ResponseEntity<ApiResponse<CarResponse>> createAd(@Validated @RequestBody AdminListingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.createAd(request)));
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

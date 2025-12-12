package com.carzavenue.backend.admin;

import com.carzavenue.backend.car.dto.CarResponse;
import com.carzavenue.backend.common.ApiResponse;
import com.carzavenue.backend.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<List<CarResponse>>> allListings() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listAllListings()));
    }

    @DeleteMapping("/listings/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteListing(@PathVariable Long id) {
        adminService.deleteListing(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> users() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listUsers()));
    }

    @PutMapping("/users/{id}/block")
    public ResponseEntity<ApiResponse<Void>> block(@PathVariable Long id, @RequestParam boolean blocked) {
        adminService.blockUser(id, blocked);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}

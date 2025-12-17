package com.carzavenue.backend.car;

import com.carzavenue.backend.car.dto.CarRequest;
import com.carzavenue.backend.car.dto.CarResponse;
import com.carzavenue.backend.common.ApiResponse;
import com.carzavenue.backend.security.SecurityUser;
import com.carzavenue.backend.user.Role;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/cars")
public class CarController {
    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CarResponse>>> list(
            @RequestParam(value = "make", required = false) String make,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "yearMin", required = false) Integer yearMin,
            @RequestParam(value = "yearMax", required = false) Integer yearMax,
            @RequestParam(value = "priceMin", required = false) Double priceMin,
            @RequestParam(value = "priceMax", required = false) Double priceMax,
            @RequestParam(value = "fuelType", required = false) String fuelType,
            @RequestParam(value = "transmission", required = false) String transmission,
            @RequestParam(value = "bodyType", required = false) String bodyType,
            @RequestParam(value = "mileageMin", required = false) Integer mileageMin,
            @RequestParam(value = "mileageMax", required = false) Integer mileageMax,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "isVip", required = false) Boolean isVip,
            @RequestParam(value = "all", required = false, defaultValue = "false") boolean all,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "newest") String sort
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                carService.list(
                        make,
                        model,
                        yearMin,
                        yearMax,
                        priceMin,
                        priceMax,
                        fuelType,
                        transmission,
                        bodyType,
                        mileageMin,
                        mileageMax,
                        location,
                        isVip,
                        all,
                        page,
                        size,
                        sort
                )
        ));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<java.util.List<CarResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(carService.listAll()));
    }

    @GetMapping(params = "all=true")
    public ResponseEntity<ApiResponse<java.util.List<CarResponse>>> listAllWithQuery(
            @RequestParam(value = "make", required = false) String make,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "yearMin", required = false) Integer yearMin,
            @RequestParam(value = "yearMax", required = false) Integer yearMax,
            @RequestParam(value = "priceMin", required = false) Double priceMin,
            @RequestParam(value = "priceMax", required = false) Double priceMax,
            @RequestParam(value = "fuelType", required = false) String fuelType,
            @RequestParam(value = "transmission", required = false) String transmission,
            @RequestParam(value = "bodyType", required = false) String bodyType,
            @RequestParam(value = "mileageMin", required = false) Integer mileageMin,
            @RequestParam(value = "mileageMax", required = false) Integer mileageMax,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "isVip", required = false) Boolean isVip,
            @RequestParam(value = "sort", required = false, defaultValue = "newest") String sort
    ) {
        // Reuse the main list service with all=true and return just the content list
        return ResponseEntity.ok(ApiResponse.ok(
                carService.list(
                        make,
                        model,
                        yearMin,
                        yearMax,
                        priceMin,
                        priceMax,
                        fuelType,
                        transmission,
                        bodyType,
                        mileageMin,
                        mileageMax,
                        location,
                        isVip,
                        true,
                        0,
                        Integer.MAX_VALUE,
                        sort
                ).getContent()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CarResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(carService.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CarResponse>> create(@AuthenticationPrincipal SecurityUser principal,
                                                           @Valid @RequestBody CarRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(carService.create(principal.getUser().getId(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CarResponse>> update(@PathVariable Long id,
                                                           @AuthenticationPrincipal SecurityUser principal,
                                                           @Valid @RequestBody CarRequest request) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        return ResponseEntity.ok(ApiResponse.ok(carService.update(id, principal.getUser().getId(), isAdmin, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @AuthenticationPrincipal SecurityUser principal) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        carService.delete(id, principal.getUser().getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{id}/vip")
    public ResponseEntity<ApiResponse<CarResponse>> markVip(@PathVariable Long id,
                                                            @RequestParam(required = false) Integer days,
                                                            @AuthenticationPrincipal SecurityUser principal) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        return ResponseEntity.ok(ApiResponse.ok(carService.markVip(id, principal.getUser().getId(), isAdmin, days)));
    }
}

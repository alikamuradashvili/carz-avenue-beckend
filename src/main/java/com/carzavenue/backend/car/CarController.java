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
            @RequestParam Optional<String> make,
            @RequestParam Optional<String> model,
            @RequestParam Optional<Integer> yearMin,
            @RequestParam Optional<Integer> yearMax,
            @RequestParam Optional<Double> priceMin,
            @RequestParam Optional<Double> priceMax,
            @RequestParam Optional<String> fuelType,
            @RequestParam Optional<String> transmission,
            @RequestParam Optional<String> bodyType,
            @RequestParam Optional<Integer> mileageMin,
            @RequestParam Optional<Integer> mileageMax,
            @RequestParam Optional<String> location,
            @RequestParam Optional<Boolean> isVip,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        return ResponseEntity.ok(ApiResponse.ok(carService.list(make, model, yearMin, yearMax, priceMin, priceMax, fuelType,
                transmission, bodyType, mileageMin, mileageMax, location, isVip, page, size, sort)));
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

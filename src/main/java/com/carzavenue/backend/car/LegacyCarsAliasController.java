package com.carzavenue.backend.car;

import com.carzavenue.backend.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Handles legacy calls like /carsall=true that some clients may use.
 */
@RestController
public class LegacyCarsAliasController {
    private final CarService carService;

    public LegacyCarsAliasController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping("/carsall=true")
    public ResponseEntity<ApiResponse<List<com.carzavenue.backend.car.dto.CarResponse>>> listAllLegacy() {
        return ResponseEntity.ok(ApiResponse.ok(carService.listAll()));
    }
}

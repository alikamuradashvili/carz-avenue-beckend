package com.carzavenue.backend.car;

import com.carzavenue.backend.car.dto.CarMultipartRequest;
import com.carzavenue.backend.car.dto.CarRequest;
import com.carzavenue.backend.car.dto.CarResponse;
import com.carzavenue.backend.common.ApiResponse;
import com.carzavenue.backend.security.SecurityUser;
import com.carzavenue.backend.user.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/manufacturers")
    @Operation(summary = "List available manufacturers")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = {
                            @ExampleObject(
                                    name = "manufacturers",
                                    value = "{\"success\":true,\"data\":[\"Toyota\",\"BMW\",\"Honda\"],\"error\":null}"
                            )
                    })
            )
    })
    public ResponseEntity<ApiResponse<java.util.List<CarManufacturer>>> listManufacturers() {
        return ResponseEntity.ok(ApiResponse.ok(carService.listManufacturers()));
    }

    @GetMapping("/models")
    @Operation(summary = "List models by manufacturer")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = {
                            @ExampleObject(
                                    name = "models",
                                    value = "{\"success\":true,\"data\":[\"Camry\",\"Corolla\"],\"error\":null}"
                            )
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = {
                            @ExampleObject(
                                    name = "invalid-manufacturer",
                                    value = "{\"success\":false,\"error\":\"manufacturer is required\"}"
                            )
                    })
            )
    })
    public ResponseEntity<ApiResponse<java.util.List<String>>> listModelsByManufacturer(
            @RequestParam("manufacturer") String manufacturer) {
        return ResponseEntity.ok(ApiResponse.ok(carService.listModelsByManufacturer(manufacturer)));
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create car (JSON)")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = {
                    @ExampleObject(
                            name = "valid",
                            value = "{\n" +
                                    "  \"title\": \"Toyota Camry 2020\",\n" +
                                    "  \"make\": \"Toyota\",\n" +
                                    "  \"model\": \"Camry\",\n" +
                                    "  \"year\": 2020,\n" +
                                    "  \"mileage\": 45000,\n" +
                                    "  \"fuelType\": \"gasoline\",\n" +
                                    "  \"transmission\": \"automatic\",\n" +
                                    "  \"bodyType\": \"sedan\",\n" +
                                    "  \"engineVolume\": 2.5,\n" +
                                    "  \"price\": 18500,\n" +
                                    "  \"color\": \"white\",\n" +
                                    "  \"description\": \"Well maintained, single owner\",\n" +
                                    "  \"location\": \"Tbilisi\",\n" +
                                    "  \"photos\": [\"http://localhost:8080/uploads/example.jpg\"]\n" +
                                    "}"
                    ),
                    @ExampleObject(
                            name = "invalid",
                            value = "{\n" +
                                    "  \"title\": \"Toyota Civic\",\n" +
                                    "  \"make\": \"Toyota\",\n" +
                                    "  \"model\": \"Civic\",\n" +
                                    "  \"year\": 2020,\n" +
                                    "  \"price\": 12000\n" +
                                    "}"
                    )
            })
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = {
                            @ExampleObject(
                                    name = "model-mismatch",
                                    value = "{\"success\":false,\"error\":\"model does not belong to make\"}"
                            )
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<CarResponse>> create(@AuthenticationPrincipal SecurityUser principal,
                                                           @Valid @RequestBody CarRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(carService.create(principal.getUser().getId(), request)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create car (multipart)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<CarResponse>> createMultipart(
            @AuthenticationPrincipal SecurityUser principal,
            @Valid @ModelAttribute CarMultipartRequest request,
            @RequestPart(value = "images", required = false)
            @Parameter(description = "Optional image files",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            MultipartFile[] images) throws java.io.IOException {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        }
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

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(carService.createWithImages(principal.getUser().getId(), mappedRequest, images)));
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

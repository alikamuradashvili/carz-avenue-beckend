package com.carzavenue.backend.car.dto;

import com.carzavenue.backend.car.PackageType;
import com.carzavenue.backend.car.VehicleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CarRequest {
    private String title;
    @NotBlank
    private String make;
    @NotBlank
    private String model;
    @NotNull
    private Integer year;
    private Integer mileage;
    private String fuelType;
    private String transmission;
    private String bodyType;
    private Double engineVolume;
    @NotNull
    private Double price;
    @NotBlank
    private String listingType;
    private PackageType packageType;
    private VehicleCategory category;
    private String vinCode;
    private String color;
    private String description;
    private String location;
    private Long imageId;
    private List<String> photos;
    private List<String> images;
}

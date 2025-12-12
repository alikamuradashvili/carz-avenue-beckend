package com.carzavenue.backend.car.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CarRequest {
    @NotBlank
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
    private String color;
    private String description;
    private String location;
    private List<String> photos;
}

package com.carzavenue.backend.car.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class CarResponse {
    Long id;
    Long ownerId;
    String title;
    String make;
    String model;
    Integer year;
    Integer mileage;
    String fuelType;
    String transmission;
    String bodyType;
    Double engineVolume;
    Double price;
    String color;
    String description;
    String location;
    List<String> photos;
    boolean isActive;
    boolean isVip;
    Instant vipExpiresAt;
    Instant createdAt;
    Instant updatedAt;
}

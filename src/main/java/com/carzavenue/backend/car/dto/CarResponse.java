package com.carzavenue.backend.car.dto;

import com.carzavenue.backend.car.PackageType;
import com.carzavenue.backend.car.VehicleCategory;
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
    String listingType;
    PackageType packageType;
    List<PackageType> packageTypes;
    VehicleCategory category;
    String make;
    String model;
    Integer year;
    Integer mileage;
    String fuelType;
    String transmission;
    String bodyType;
    Double engineVolume;
    Double price;
    String contactPhone;
    String sellerName;
    String sellerUsername;
    String sellerPhone;
    String color;
    String description;
    String location;
    Long imageId;
    List<String> photos;
    // Alias for clients expecting "images"
    List<String> images;
    boolean isActive;
    boolean isVip;
    Instant vipExpiresAt;
    Instant createdAt;
    Instant updatedAt;
}

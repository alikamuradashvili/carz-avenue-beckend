package com.carzavenue.backend.car;

import com.carzavenue.backend.car.dto.CarRequest;
import com.carzavenue.backend.car.dto.CarResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CarMapper {
    private static final String DEFAULT_IMAGE_URL = "http://localhost:8080/images/default-car.jpg";

    public static CarListing fromRequest(CarRequest request) {
        CarListing car = new CarListing();
        car.setStatus(AdStatus.ACTIVE);
        car.setActive(true);
        car.setMake(request.getMake());
        car.setModel(request.getModel());
        car.setVinCode(request.getVinCode());
        car.setYear(request.getYear());
        car.setMileage(request.getMileage());
        car.setFuelType(request.getFuelType());
        car.setTransmission(request.getTransmission());
        car.setBodyType(request.getBodyType());
        car.setEngineVolume(request.getEngineVolume());
        car.setPrice(request.getPrice());
        car.setColor(request.getColor());
        car.setDescription(request.getDescription());
        car.setLocation(request.getLocation());
        car.setImageId(request.getImageId());
        List<String> photos = request.getPhotos() != null ? request.getPhotos() : request.getImages();
        if (photos != null) {
            car.setPhotos(sanitizePhotos(photos));
        }
        return car;
    }

    public static void updateEntity(CarListing car, CarRequest request) {
        car.setMake(request.getMake());
        car.setModel(request.getModel());
        car.setVinCode(request.getVinCode());
        car.setYear(request.getYear());
        car.setMileage(request.getMileage());
        car.setFuelType(request.getFuelType());
        car.setTransmission(request.getTransmission());
        car.setBodyType(request.getBodyType());
        car.setEngineVolume(request.getEngineVolume());
        car.setPrice(request.getPrice());
        car.setColor(request.getColor());
        car.setDescription(request.getDescription());
        car.setLocation(request.getLocation());
        car.setImageId(request.getImageId());
        List<String> photos = request.getPhotos() != null ? request.getPhotos() : request.getImages();
        if (photos != null) {
            car.setPhotos(sanitizePhotos(photos));
        }
    }

    public static CarResponse toResponse(CarListing car) {
        List<String> photos = sanitizePhotos(car.getPhotos());
        return CarResponse.builder()
                .id(car.getId())
                .ownerId(car.getOwner().getId())
                .title(car.getTitle())
                .make(car.getMake())
                .model(car.getModel())
                .year(car.getYear())
                .mileage(car.getMileage())
                .fuelType(car.getFuelType())
                .transmission(car.getTransmission())
                .bodyType(car.getBodyType())
                .engineVolume(car.getEngineVolume())
                .price(car.getPrice())
                .color(car.getColor())
                .description(car.getDescription())
                .location(car.getLocation())
                .imageId(car.getImageId())
                .photos(photos)
                .images(photos)
                .isActive(car.isActive())
                .isVip(car.isVip())
                .vipExpiresAt(car.getVipExpiresAt())
                .createdAt(car.getCreatedAt())
                .updatedAt(car.getUpdatedAt())
                .build();
    }

    private static List<String> sanitizePhotos(List<String> photos) {
        if (photos == null || photos.isEmpty()) {
            return new ArrayList<>(List.of(DEFAULT_IMAGE_URL));
        }

        List<String> sanitized = photos.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .map(CarMapper::replaceBlockedSources)
                .toList();

        if (sanitized.isEmpty()) {
            return new ArrayList<>(List.of(DEFAULT_IMAGE_URL));
        }

        return new ArrayList<>(sanitized);
    }

    private static String replaceBlockedSources(String url) {
        String lower = url.toLowerCase();
        if (lower.contains("photos.app.goo.gl") || lower.contains("photos.google.com")) {
            return DEFAULT_IMAGE_URL;
        }
        return url;
    }
}

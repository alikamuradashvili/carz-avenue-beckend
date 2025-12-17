package com.carzavenue.backend.car;

import com.carzavenue.backend.car.dto.CarRequest;
import com.carzavenue.backend.car.dto.CarResponse;

import java.util.List;

public class CarMapper {
    public static CarListing fromRequest(CarRequest request) {
        CarListing car = new CarListing();
        car.setTitle(request.getTitle());
        car.setMake(request.getMake());
        car.setModel(request.getModel());
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
        List<String> photos = request.getPhotos() != null ? request.getPhotos() : request.getImages();
        if (photos != null) {
            car.setPhotos(photos);
        }
        return car;
    }

    public static void updateEntity(CarListing car, CarRequest request) {
        car.setTitle(request.getTitle());
        car.setMake(request.getMake());
        car.setModel(request.getModel());
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
        List<String> photos = request.getPhotos() != null ? request.getPhotos() : request.getImages();
        if (photos != null) {
            car.setPhotos(photos);
        }
    }

    public static CarResponse toResponse(CarListing car) {
        List<String> photos = car.getPhotos();
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
                .photos(photos)
                .images(photos)
                .isActive(car.isActive())
                .isVip(car.isVip())
                .vipExpiresAt(car.getVipExpiresAt())
                .createdAt(car.getCreatedAt())
                .updatedAt(car.getUpdatedAt())
                .build();
    }
}

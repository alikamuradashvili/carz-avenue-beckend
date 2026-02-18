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
        car.setListingType(request.getListingType());
        applyPackageTypes(car, request.getPackageType(), request.getPackageTypes());
        if (request.getCategory() != null) {
            car.setCategory(request.getCategory());
        }
        car.setStatus(AdStatus.ACTIVE);
        car.setActive(true);
        car.setMake(request.getMake());
        car.setModel(request.getModel());
        car.setContactPhone(request.getContactPhone());
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
        car.setListingType(request.getListingType());
        applyPackageTypes(car, request.getPackageType(), request.getPackageTypes());
        if (request.getCategory() != null) {
            car.setCategory(request.getCategory());
        }
        car.setMake(request.getMake());
        car.setModel(request.getModel());
        car.setContactPhone(request.getContactPhone());
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
        String sellerName = car.getOwner() != null ? car.getOwner().getName() : null;
        String sellerUsername = null;
        if (car.getOwner() != null && car.getOwner().getEmail() != null) {
            String email = car.getOwner().getEmail();
            int at = email.indexOf("@");
            sellerUsername = at > 0 ? email.substring(0, at) : email;
        }
        if (sellerName == null || sellerName.isBlank()) {
            sellerName = sellerUsername;
        }
        String sellerPhone = car.getContactPhone();
        if ((sellerPhone == null || sellerPhone.isBlank()) && car.getOwner() != null) {
            sellerPhone = car.getOwner().getPhoneNumber();
        }
        List<PackageType> packageTypes = car.getPackageTypes();
        if ((packageTypes == null || packageTypes.isEmpty()) && car.getPackageType() != null) {
            packageTypes = List.of(car.getPackageType());
        }

        return CarResponse.builder()
                .id(car.getId())
                .ownerId(car.getOwner().getId())
                .title(car.getTitle())
                .listingType(car.getListingType())
                .packageType(car.getPackageType())
                .packageTypes(packageTypes)
                .category(car.getCategory())
                .make(car.getMake())
                .model(car.getModel())
                .year(car.getYear())
                .mileage(car.getMileage())
                .fuelType(car.getFuelType())
                .transmission(car.getTransmission())
                .bodyType(car.getBodyType())
                .engineVolume(car.getEngineVolume())
                .price(car.getPrice())
                .contactPhone(car.getContactPhone())
                .sellerName(sellerName)
                .sellerUsername(sellerUsername)
                .sellerPhone(sellerPhone)
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

    private static void applyPackageTypes(CarListing car, PackageType packageType, List<PackageType> packageTypes) {
        if (packageTypes != null) {
            List<PackageType> normalized = packageTypes.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            car.setPackageTypes(new ArrayList<>(normalized));
            if (!normalized.isEmpty()) {
                car.setPackageType(normalized.get(0));
            }
        }
        if (packageType != null) {
            car.setPackageType(packageType);
            if (packageTypes == null || packageTypes.isEmpty()) {
                car.setPackageTypes(new ArrayList<>(List.of(packageType)));
            }
        }
        if ((car.getPackageTypes() == null || car.getPackageTypes().isEmpty()) && car.getPackageType() != null) {
            car.setPackageTypes(new ArrayList<>(List.of(car.getPackageType())));
        }
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

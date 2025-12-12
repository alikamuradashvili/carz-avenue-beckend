package com.carzavenue.backend.car;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Objects;

public class CarSpecifications {
    public static Specification<CarListing> active() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<CarListing> make(String make) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("make")), make.toLowerCase());
    }

    public static Specification<CarListing> model(String model) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("model")), model.toLowerCase());
    }

    public static Specification<CarListing> yearMin(Integer year) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("year"), year);
    }

    public static Specification<CarListing> yearMax(Integer year) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("year"), year);
    }

    public static Specification<CarListing> priceMin(Double price) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), price);
    }

    public static Specification<CarListing> priceMax(Double price) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), price);
    }

    public static Specification<CarListing> fuelType(String fuelType) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("fuelType")), fuelType.toLowerCase());
    }

    public static Specification<CarListing> transmission(String transmission) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("transmission")), transmission.toLowerCase());
    }

    public static Specification<CarListing> bodyType(String bodyType) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("bodyType")), bodyType.toLowerCase());
    }

    public static Specification<CarListing> mileageMin(Integer mileage) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("mileage"), mileage);
    }

    public static Specification<CarListing> mileageMax(Integer mileage) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("mileage"), mileage);
    }

    public static Specification<CarListing> location(String location) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
    }

    public static Specification<CarListing> vip(Boolean vip) {
        if (vip == null) return null;
        return (root, query, cb) -> cb.equal(root.get("isVip"), vip);
    }

    public static Specification<CarListing> vipNotExpired() {
        return (root, query, cb) -> cb.or(cb.isNull(root.get("vipExpiresAt")),
                cb.greaterThan(root.get("vipExpiresAt"), Instant.now()));
    }
}

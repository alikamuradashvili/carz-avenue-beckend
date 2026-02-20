package com.carzavenue.backend.car;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CarListingRepository extends JpaRepository<CarListing, Long>, JpaSpecificationExecutor<CarListing> {
    long countByOwnerIdAndIsActiveTrue(Long ownerId);
    long countByOwnerIdAndIsActiveFalse(Long ownerId);

    @Query("select distinct c.model from CarListing c where lower(c.make) = lower(:make) and c.isActive = true")
    List<String> findDistinctModelsByMake(@Param("make") String make);

    @Query("select distinct c.make from CarListing c where c.make is not null order by c.make")
    List<String> findDistinctMakes();

    @Query("select distinct c.make from CarListing c where c.make is not null and c.isActive = true and (:category is null or c.category = :category) order by c.make")
    List<String> findDistinctMakesByCategory(@Param("category") VehicleCategory category);

    @Query("select count(c) > 0 from CarListing c where lower(c.make) = lower(:make) and c.isActive = true")
    boolean existsActiveMake(@Param("make") String make);

    @Query("select count(c) > 0 from CarListing c where lower(c.make) = lower(:make) and lower(c.model) = lower(:model) and c.isActive = true")
    boolean existsActiveModelByMake(@Param("make") String make, @Param("model") String model);

    @Query("select distinct c.model from CarListing c where c.model is not null order by c.model")
    List<String> findDistinctModels();

    @Query("select distinct c.model from CarListing c where lower(c.make) = lower(:make) and c.isActive = true and (:category is null or c.category = :category) order by c.model")
    List<String> findDistinctModelsByMakeAndCategory(@Param("make") String make, @Param("category") VehicleCategory category);

    @Query("select distinct c.owner.email from CarListing c where c.owner.email is not null order by c.owner.email")
    List<String> findDistinctOwnerEmails();

    @Query("select distinct c.owner.name from CarListing c where c.owner.name is not null order by c.owner.name")
    List<String> findDistinctOwnerNames();

    @Query("select distinct c.year from CarListing c where c.year is not null order by c.year")
    List<Integer> findDistinctYears();

    @Query("select distinct c.listingType from CarListing c where c.listingType is not null and c.listingType <> '' order by c.listingType")
    List<String> findDistinctListingTypes();

    @Query("select distinct p from CarListing c join c.packageTypes p order by p")
    List<PackageType> findDistinctPackageTypes();

    @Query("select distinct c.category from CarListing c where c.category is not null order by c.category")
    List<VehicleCategory> findDistinctCategories();

    @Query("select distinct c.fuelType from CarListing c where c.fuelType is not null and c.fuelType <> '' order by c.fuelType")
    List<String> findDistinctFuelTypes();

    @Query("select distinct c.transmission from CarListing c where c.transmission is not null and c.transmission <> '' order by c.transmission")
    List<String> findDistinctTransmissions();

    @Query("select distinct c.bodyType from CarListing c where c.bodyType is not null and c.bodyType <> '' order by c.bodyType")
    List<String> findDistinctBodyTypes();

    @Query("select distinct c.color from CarListing c where c.color is not null and c.color <> '' order by c.color")
    List<String> findDistinctColors();

    @Query("select distinct c.engineVolume from CarListing c where c.engineVolume is not null order by c.engineVolume")
    List<Double> findDistinctEngineVolumes();

    @Query("select distinct c.mileage from CarListing c where c.mileage is not null order by c.mileage")
    List<Integer> findDistinctMileages();

    @Query("select min(c.price) from CarListing c")
    Double findMinPrice();

    @Query("select max(c.price) from CarListing c")
    Double findMaxPrice();
}

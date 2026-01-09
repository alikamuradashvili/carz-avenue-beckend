package com.carzavenue.backend.car;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CarListingRepository extends JpaRepository<CarListing, Long>, JpaSpecificationExecutor<CarListing> {
    @Query("select distinct c.model from CarListing c where lower(c.make) = lower(:make) and c.isActive = true")
    List<String> findDistinctModelsByMake(@Param("make") String make);

    @Query("select distinct c.make from CarListing c where c.isActive = true")
    List<String> findDistinctMakes();

    @Query("select count(c) > 0 from CarListing c where lower(c.make) = lower(:make) and c.isActive = true")
    boolean existsActiveMake(@Param("make") String make);

    @Query("select count(c) > 0 from CarListing c where lower(c.make) = lower(:make) and lower(c.model) = lower(:model) and c.isActive = true")
    boolean existsActiveModelByMake(@Param("make") String make, @Param("model") String model);
}

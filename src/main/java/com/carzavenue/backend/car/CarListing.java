package com.carzavenue.backend.car;

import com.carzavenue.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "car_listing")
public class CarListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String title;

    @Column(name = "listing_type")
    private String listingType;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(name = "vin_code", length = 17)
    private String vinCode;

    private Integer year;
    private Integer mileage;
    private String fuelType;
    private String transmission;
    private String bodyType;
    private Double engineVolume;
    private String color;
    private Double price;
    @Column(length = 4000)
    private String description;
    private String location;

    @Column(name = "image_id")
    private Long imageId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "car_photos", joinColumns = @JoinColumn(name = "car_id"))
    @Column(name = "url")
    private List<String> photos = new ArrayList<>();

    private boolean isActive = true;
    private boolean isVip = false;
    private Instant vipExpiresAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageType packageType = PackageType.ECONOM;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "car_listing_package_types", joinColumns = @JoinColumn(name = "car_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false)
    private List<PackageType> packageTypes = new ArrayList<>();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleCategory category = VehicleCategory.OTHER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdStatus status = AdStatus.ACTIVE;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}

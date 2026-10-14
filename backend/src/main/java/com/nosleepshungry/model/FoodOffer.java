package com.nosleepshungry.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "food_offers")
public class FoodOffer {

    public enum FoodType { VEG, NON_VEG, MIXED }

    public enum Status { OPEN, ASSIGNED, IN_TRANSIT, COMPLETED, REJECTED, EXPIRED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vendorName;
    private String submitterName;
    private String submitterPhone;
    private String area;
    private Long localityId;
    private String pickupAddress;
    private String description;
    private int servings;

    @Enumerated(EnumType.STRING)
    private FoodType foodType;

    private LocalDateTime cookedAt;
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    private Status status = Status.OPEN;

    private LocalDateTime createdAt = LocalDateTime.now();

    protected FoodOffer() { }

    public FoodOffer(String vendorName, String submitterName, String submitterPhone, String area,
                     String pickupAddress, String description, int servings, FoodType foodType,
                     LocalDateTime cookedAt, LocalDateTime expiresAt) {
        this.vendorName = vendorName;
        this.submitterName = submitterName;
        this.submitterPhone = submitterPhone;
        this.area = area;
        this.pickupAddress = pickupAddress;
        this.description = description;
        this.servings = servings;
        this.foodType = foodType;
        this.cookedAt = cookedAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public void setStatus(Status status) { this.status = status; }
    public Long getLocalityId() { return localityId; }
    public Status getStatus() { return status; }
    public java.time.LocalDateTime getExpiresAt() { return expiresAt; }
    public int getServings() { return servings; }
    public String getDescription() { return description; }
    public String getPickupAddress() { return pickupAddress; }
    public String getArea() { return area; }
    public void setLocalityId(Long localityId) { this.localityId = localityId; }
}

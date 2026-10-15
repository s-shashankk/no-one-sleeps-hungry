package com.mealup.requests;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.mealup.common.exception.ConflictException;

import jakarta.persistence.*;

/** Either "people need food" or "we have surplus food". One table, one simple life cycle. */
@Entity
@Table(name = "requests")
public class FoodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private RequestType type;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_phone")
    private String contactPhone;

    private String location;

    /** NEED_FOOD: people to feed. SURPLUS_FOOD: meals on offer. */
    private int quantity;

    private String details;

    @Column(name = "meals_delivered")
    private Integer mealsDelivered;

    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    protected FoodRequest() { }

    public FoodRequest(RequestType type, String contactName, String contactPhone, String location,
                       int quantity, String details) {
        this.type = type;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.location = location;
        this.quantity = quantity;
        this.details = details;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /** Fix typos in the details. The kind of request and its status are not changed here. */
    public void editDetails(String contactName, String contactPhone, String location, int quantity, String details) {
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.location = location;
        this.quantity = quantity;
        this.details = details;
    }

    // ---- the only ways a status changes ----------------------------------------------------

    /** Food was delivered to people: only for "need food" requests that are still pending. */
    public void complete(int meals, String note) {
        if (type != RequestType.NEED_FOOD) {
            throw new ConflictException("Only a request for food can be completed. Accept a surplus offer instead.");
        }
        requirePending();
        this.mealsDelivered = meals;
        close(RequestStatus.COMPLETED, note);
    }

    /** Surplus food offer accepted: the admin will send a team to collect it. */
    public void accept(String note) {
        if (type != RequestType.SURPLUS_FOOD) {
            throw new ConflictException("Only a surplus food offer can be accepted. Complete a food request instead.");
        }
        requirePending();
        close(RequestStatus.ACCEPTED, note);
    }

    /** "Unable to serve" (need food) or "unable to collect" (surplus food). */
    public void reject(String note) {
        requirePending();
        close(RequestStatus.REJECTED, note);
    }

    /** Puts a closed request back to pending, e.g. after a mistake. Delivered meals are cleared. */
    public void reopen() {
        if (status == RequestStatus.PENDING) {
            throw new ConflictException("This request is already pending.");
        }
        this.status = RequestStatus.PENDING;
        this.mealsDelivered = null;
        this.closedAt = null;
    }

    private void requirePending() {
        if (status != RequestStatus.PENDING) {
            throw new ConflictException("This request is already " + status.name().toLowerCase()
                    + ". Reopen it first if it needs to change.");
        }
    }

    private void close(RequestStatus to, String note) {
        this.status = to;
        this.note = note;
        this.closedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public RequestType getType() { return type; }
    public RequestStatus getStatus() { return status; }
    public String getContactName() { return contactName; }
    public String getContactPhone() { return contactPhone; }
    public String getLocation() { return location; }
    public int getQuantity() { return quantity; }
    public String getDetails() { return details; }
    public Integer getMealsDelivered() { return mealsDelivered; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
}

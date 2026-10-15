package com.mealup.vendors;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.*;

/** A hotel, restaurant, caterer or event host the admin phones to ask about surplus food. */
@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String location;

    private String notes;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Vendor() { }

    public Vendor(String name, String contactPerson, String phoneNumber, String location, String notes) {
        this.name = name;
        this.contactPerson = contactPerson;
        this.phoneNumber = phoneNumber;
        this.location = location;
        this.notes = notes;
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

    public void change(String name, String contactPerson, String phoneNumber, String location, String notes) {
        this.name = name;
        this.contactPerson = contactPerson;
        this.phoneNumber = phoneNumber;
        this.location = location;
        this.notes = notes;
    }

    public void setActive(boolean active) { this.active = active; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getContactPerson() { return contactPerson; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getLocation() { return location; }
    public String getNotes() { return notes; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

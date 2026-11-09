package com.mealup.users;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.*;

/** Someone who can sign in: an admin, or a volunteer who signed up. (Coworkers and vendors are plain records; they do not sign in.) */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role = Role.ADMIN;

    /** For hotel staff: the hotel they work for. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private com.mealup.vendors.Vendor vendor;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    protected User() { }

    public User(String fullName, String phoneNumber, String email, String passwordHash) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /** A volunteer account. */
    public static User volunteer(String fullName, String phoneNumber, String email, String passwordHash) {
        User u = new User(fullName, phoneNumber, email, passwordHash);
        u.role = Role.VOLUNTEER;
        return u;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /** The admin gives this person hotel-staff access for one hotel. */
    public void makeHotelStaff(com.mealup.vendors.Vendor hotel) { this.role = Role.HOTEL; this.vendor = hotel; }

    /** Back to an ordinary volunteer. */
    public void makeVolunteer() { this.role = Role.VOLUNTEER; this.vendor = null; }

    public void recordLogin() { this.lastLoginAt = LocalDateTime.now(ZoneOffset.UTC); }
    public void setActive(boolean active) { this.active = active; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public com.mealup.vendors.Vendor getVendor() { return vendor; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
}

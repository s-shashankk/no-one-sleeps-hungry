package com.mealup.joins;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.mealup.common.exception.ConflictException;

import jakarta.persistence.*;

/** Someone who asked to join the MealUp community. */
@Entity
@Table(name = "join_requests")
public class JoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String email;

    private String location;

    private String message;

    @Enumerated(EnumType.STRING)
    private JoinStatus status = JoinStatus.NEW;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "contacted_at")
    private LocalDateTime contactedAt;

    protected JoinRequest() { }

    public JoinRequest(String name, String phoneNumber, String email, String location, String message) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.location = location;
        this.message = message;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void markContacted() {
        if (status == JoinStatus.CONTACTED) {
            throw new ConflictException("Already marked as contacted.");
        }
        status = JoinStatus.CONTACTED;
        contactedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void reopen() {
        if (status == JoinStatus.NEW) {
            throw new ConflictException("Already marked as new.");
        }
        status = JoinStatus.NEW;
        contactedAt = null;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }
    public String getLocation() { return location; }
    public String getMessage() { return message; }
    public JoinStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getContactedAt() { return contactedAt; }
}

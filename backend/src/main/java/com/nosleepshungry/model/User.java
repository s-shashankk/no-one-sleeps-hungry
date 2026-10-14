package com.nosleepshungry.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    public enum Role { SUPER_ADMIN, COORDINATOR }

    public enum Status { ACTIVE, SUSPENDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String phone;

    @JsonIgnore
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "coordinator_localities", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "locality_id")
    private Set<Long> localityIds = new HashSet<>();

    protected User() { }

    public User(String name, String email, String phone, String passwordHash, Role role) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public Status getStatus() { return status; }
    public Set<Long> getLocalityIds() { return localityIds; }

    public void setStatus(Status status) { this.status = status; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public void setLocalityIds(Set<Long> ids) {
        this.localityIds.clear();
        if (ids != null) this.localityIds.addAll(ids);
    }
}

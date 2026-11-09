package com.mealup.areas;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.*;

/** A neighbourhood MealUp works in (or will soon), e.g. Indiranagar, Bengaluru. */
@Entity
@Table(name = "areas")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    private String name;

    /** true = volunteers can collect food here; false = "coming soon". */
    @Column(name = "is_open")
    private boolean open;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Area() { }

    public Area(String city, String name, boolean open, int sortOrder) {
        this.city = city;
        this.name = name;
        this.open = open;
        this.sortOrder = sortOrder;
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

    public void change(String city, String name, boolean open) {
        this.city = city;
        this.name = name;
        this.open = open;
    }

    public Long getId() { return id; }
    public String getCity() { return city; }
    public String getName() { return name; }
    public boolean isOpen() { return open; }
    public int getSortOrder() { return sortOrder; }
}

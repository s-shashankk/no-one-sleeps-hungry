package com.nosleepshungry.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "localities")
public class Locality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String city;
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Locality() { }

    public Locality(String name, String city) {
        this.name = name;
        this.city = city;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
}

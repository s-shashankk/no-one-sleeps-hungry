package com.nosleepshungry.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "volunteer_applications")
public class VolunteerApplication {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phone;
    private String email;
    private String area;
    private Long localityId;
    private String collegeOrOccupation;
    private String message;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    protected VolunteerApplication() { }

    public VolunteerApplication(String name, String phone, String email, String area,
                                String collegeOrOccupation, String message) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.area = area;
        this.collegeOrOccupation = collegeOrOccupation;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setStatus(Status status) { this.status = status; }
    public Status getStatus() { return status; }
    public Long getLocalityId() { return localityId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public void setLocalityId(Long localityId) { this.localityId = localityId; }
}

package com.nosleepshungry.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "hunger_reports")
public class HungerReport {

    public enum Status { NEW, VERIFIED, SCHEDULED, SERVED, INVALID, RECURRING }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String area;
    private Long localityId;
    private String locationText;
    private Double latitude;
    private Double longitude;
    private int peopleCount;
    private String notes;
    private String reporterName;
    private String reporterPhone;

    @Enumerated(EnumType.STRING)
    private Status status = Status.NEW;

    private LocalDateTime createdAt = LocalDateTime.now();

    protected HungerReport() { }

    public HungerReport(String area, String locationText, Double latitude, Double longitude,
                        int peopleCount, String notes, String reporterName, String reporterPhone) {
        this.area = area;
        this.locationText = locationText;
        this.latitude = latitude;
        this.longitude = longitude;
        this.peopleCount = peopleCount;
        this.notes = notes;
        this.reporterName = reporterName;
        this.reporterPhone = reporterPhone;
    }

    public Long getId() { return id; }
    public void setStatus(Status status) { this.status = status; }
    public Long getLocalityId() { return localityId; }
    public Status getStatus() { return status; }
    public int getPeopleCount() { return peopleCount; }
    public String getLocationText() { return locationText; }
    public String getArea() { return area; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public void setLocalityId(Long localityId) { this.localityId = localityId; }
}

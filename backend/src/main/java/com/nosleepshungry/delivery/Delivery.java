package com.nosleepshungry.delivery;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(name = "deliveries")
public class Delivery {

    public enum Status { ASSIGNED, PICKED_UP, DELIVERED, FAILED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long foodOfferId;
    private Long localityId;
    private String createdBy;
    private String volunteerName;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ASSIGNED;

    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private Integer peopleServed;
    private String notes;
    private LocalDateTime createdAt = LocalDateTime.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "delivery_reports", joinColumns = @JoinColumn(name = "delivery_id"))
    @Column(name = "hunger_report_id")
    private Set<Long> hungerReportIds = new HashSet<>();

    protected Delivery() { }

    public Delivery(Long foodOfferId, Long localityId, String createdBy, String volunteerName,
                    String notes, Set<Long> hungerReportIds) {
        this.foodOfferId = foodOfferId;
        this.localityId = localityId;
        this.createdBy = createdBy;
        this.volunteerName = volunteerName;
        this.notes = notes;
        this.hungerReportIds.addAll(hungerReportIds);
    }

    public Long getId() { return id; }
    public Long getFoodOfferId() { return foodOfferId; }
    public Long getLocalityId() { return localityId; }
    public String getCreatedBy() { return createdBy; }
    public String getVolunteerName() { return volunteerName; }
    public Status getStatus() { return status; }
    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public Integer getPeopleServed() { return peopleServed; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Set<Long> getHungerReportIds() { return hungerReportIds; }

    public void setStatus(Status status) { this.status = status; }
    public void setPickedUpAt(LocalDateTime t) { this.pickedUpAt = t; }
    public void setDeliveredAt(LocalDateTime t) { this.deliveredAt = t; }
    public void setPeopleServed(Integer n) { this.peopleServed = n; }
    public void setNotes(String notes) { this.notes = notes; }
}

package com.mealup.pickups;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.mealup.areas.Area;
import com.mealup.common.exception.ConflictException;
import com.mealup.users.User;
import com.mealup.vendors.Vendor;

import jakarta.persistence.*;

/** A volunteer's promise to collect food from one hotel, followed through to "plates served". */
@Entity
@Table(name = "pickups")
public class Pickup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id")
    private Area area;

    /** The volunteer's account (empty for old or sample rows). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_user_id")
    private User volunteer;

    @Column(name = "volunteer_name")
    private String volunteerName;

    @Column(name = "volunteer_phone")
    private String volunteerPhone;

    @Column(name = "deliver_to")
    private String deliverTo;

    /** How many people the volunteer said they will feed (reported before collecting). */
    @Column(name = "people_count")
    private int peopleCount = 1;

    @Enumerated(EnumType.STRING)
    private PickupStatus status = PickupStatus.PLEDGED;

    @Column(name = "plates_served")
    private Integer platesServed;

    /** Who closed it: ADMIN or HOTEL (empty while open or cancelled). */
    @Column(name = "closed_by")
    private String closedBy;

    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    protected Pickup() { }

    public Pickup(Vendor vendor, Area area, User volunteer, String volunteerName, String volunteerPhone,
                  String deliverTo, int peopleCount) {
        this.vendor = vendor;
        this.area = area;
        this.volunteer = volunteer;
        this.peopleCount = peopleCount;
        this.volunteerName = volunteerName;
        this.volunteerPhone = volunteerPhone;
        this.deliverTo = deliverTo;
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

    // ---- the only ways a status changes ----------------------------------------------------

    /** The admin has told the hotel that this volunteer is coming. */
    public void confirm() {
        if (status != PickupStatus.PLEDGED) {
            throw new ConflictException("Only a new pickup can be marked as 'hotel informed'.");
        }
        status = PickupStatus.CONFIRMED;
    }

    /** The food was collected and served (recorded by the admin). */
    public void complete(int plates, String note) {
        complete(plates, note, "ADMIN");
    }

    /** The food was handed over and served. closedBy is ADMIN or HOTEL. */
    public void complete(int plates, String note, String closedBy) {
        requireOpen();
        this.closedBy = closedBy;
        this.platesServed = plates;
        this.note = note;
        this.status = PickupStatus.COMPLETED;
        this.closedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void cancel(String note) {
        requireOpen();
        this.note = note;
        this.status = PickupStatus.CANCELLED;
        this.closedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /** Puts a finished pickup back to new, e.g. after a mistake. Plates served are cleared. */
    public void reopen() {
        if (status != PickupStatus.COMPLETED && status != PickupStatus.CANCELLED) {
            throw new ConflictException("This pickup is still open.");
        }
        status = PickupStatus.PLEDGED;
        platesServed = null;
        closedBy = null;
        closedAt = null;
    }

    private void requireOpen() {
        if (status != PickupStatus.PLEDGED && status != PickupStatus.CONFIRMED) {
            throw new ConflictException("This pickup is already " + status.name().toLowerCase()
                    + ". Reopen it first if it needs to change.");
        }
    }

    public Long getId() { return id; }
    public Vendor getVendor() { return vendor; }
    public Area getArea() { return area; }
    public User getVolunteer() { return volunteer; }
    public int getPeopleCount() { return peopleCount; }
    public String getVolunteerName() { return volunteerName; }
    public String getVolunteerPhone() { return volunteerPhone; }
    public String getDeliverTo() { return deliverTo; }
    public PickupStatus getStatus() { return status; }
    public Integer getPlatesServed() { return platesServed; }
    public String getClosedBy() { return closedBy; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
}

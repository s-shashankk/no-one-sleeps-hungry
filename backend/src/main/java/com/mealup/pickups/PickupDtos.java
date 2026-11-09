package com.mealup.pickups;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PickupDtos {

    private PickupDtos() { }

    /** What the admin sees: the hotel (with its phone), the volunteer's account and how many people they will feed. */
    public record PickupResponse(Long id, PickupStatus status,
                                 Long vendorId, String vendorName, String vendorPhone, String vendorLocation,
                                 Long areaId, String areaName, String city,
                                 Long volunteerUserId, String volunteerEmail,
                                 String volunteerName, String volunteerPhone, String deliverTo,
                                 int peopleCount, Integer platesServed, String note,
                                 Instant createdAt, Instant closedAt, String closedBy) { }

    /** What a volunteer sees about their own pickups (no hotel phone number, no admin notes). */
    public record VolunteerPickupResponse(Long id, PickupStatus status, String hotelName, String hotelLocation,
                                          String areaName, int peopleCount, String deliverTo, Integer platesServed,
                                          Instant createdAt, Instant closedAt, String closedBy, String hotelMapUrl) { }

    /** "I will collect food from this hotel for about this many people." Sent by a signed-in volunteer. */
    public record PledgeRequest(
            @NotNull Long vendorId,
            @NotNull @Min(value = 1, message = "Enter how many people you will feed") @Max(5000) Integer peopleCount,
            @Size(max = 500) String deliverTo) { }

    public record CompletePickupRequest(@NotNull @Min(1) @Max(1000000) Integer platesServed,
                                        @Size(max = 500) String note) { }

    public record PickupNoteRequest(@Size(max = 500) String note) { }
}

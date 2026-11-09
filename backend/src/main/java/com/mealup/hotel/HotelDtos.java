package com.mealup.hotel;

import java.time.Instant;

import com.mealup.pickups.PickupStatus;

import jakarta.validation.constraints.Size;

public final class HotelDtos {

    private HotelDtos() { }

    /** The hotel's numbers for today. waiting = volunteers booked and not yet accepted. */
    public record HotelSummary(String hotelName, String hotelLocation, int dailyLimit, long bookedToday, long waiting) { }

    /** One booking at this hotel. closedBy says who accepted it (HOTEL, or ADMIN on the hotel's behalf). */
    public record HotelPickup(Long id, PickupStatus status, String volunteerName, String volunteerPhone,
                              int peopleCount, String deliverTo, String closedBy,
                              Instant createdAt, Instant closedAt) { }

    public record DeclineRequest(@Size(max = 500) String note) { }
}

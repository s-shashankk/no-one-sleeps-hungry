package com.mealup.pickups;

/**
 * PLEDGED   a volunteer says they will collect food from a hotel (waiting for the admin).
 * CONFIRMED the admin has informed the hotel.
 * COMPLETED the food was collected and served; plates served are recorded.
 * CANCELLED it did not happen (reason in the note).
 */
public enum PickupStatus {
    PLEDGED,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}

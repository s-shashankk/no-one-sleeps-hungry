package com.mealup.users;

public enum Role {
    /** Runs MealUp: sees and changes everything in the admin pages. */
    ADMIN,
    /** A person who signed up to collect and deliver food. Sees only their own deliveries. */
    VOLUNTEER,
    /** Staff of a partner hotel (linked to that hotel). Sees and confirms pickups booked at their own hotel only. */
    HOTEL
}

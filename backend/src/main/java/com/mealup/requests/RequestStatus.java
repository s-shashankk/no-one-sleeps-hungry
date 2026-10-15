package com.mealup.requests;

/**
 * NEED_FOOD:    PENDING -> COMPLETED (meals delivered recorded)  or REJECTED (unable to serve).
 * SURPLUS_FOOD: PENDING -> ACCEPTED (a team will collect)        or REJECTED (unable to collect).
 * Any closed request can be reopened (back to PENDING) to fix a mistake.
 */
public enum RequestStatus {
    PENDING,
    ACCEPTED,
    COMPLETED,
    REJECTED
}

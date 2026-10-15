package com.mealup.dashboard;

public final class DashboardDtos {

    private DashboardDtos() { }

    public record Summary(long totalMealsDelivered, long activeVendors, long activeCoworkers, long pendingRequests) { }
}

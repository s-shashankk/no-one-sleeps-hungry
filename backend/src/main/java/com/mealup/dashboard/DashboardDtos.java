package com.mealup.dashboard;

public final class DashboardDtos {

    private DashboardDtos() { }

    /**
     * totalMealsDelivered: plates from completed pickups + meals delivered for completed food requests.
     * newPickups / newJoinRequests are what is waiting for the admin's attention.
     */
    public record Summary(long totalMealsDelivered, long activeVendors, long activeCoworkers, long pendingRequests,
                          long newPickups, long newJoinRequests, long openAreas, long cities, long volunteers, long newMessages) { }
}

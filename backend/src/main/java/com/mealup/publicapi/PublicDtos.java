package com.mealup.publicapi;

import java.util.List;

public final class PublicDtos {

    private PublicDtos() { }

    public record PublicMessage(String message) { }

    /** An area as visitors see it. */
    public record PublicArea(Long id, String name, String city, boolean open, long hotels) { }

    /** A partner hotel as visitors see it: name and address only, never the phone number. */
    /** spotsLeft = how many more volunteers can book this hotel today. */
    public record PublicHotel(Long id, String name, String location, int spotsLeft, String mapUrl) { }

    /** A partner hotel for the home-page slider. logoVersion is null when the hotel has no logo yet. */
    public record PublicPartner(Long id, String name, String areaName, Long logoVersion) { }

    public record AreaImpact(Long id, String name, String city, boolean open, long hotels, long pickupMeals) { }

    /** The numbers on the public dashboard. */
    public record Impact(long mealsServed, long cities, long areasOpen, long hotelPartners,
                         long pickupsCompleted, List<AreaImpact> areas) { }
}

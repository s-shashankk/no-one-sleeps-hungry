package com.mealup.vendors;

import java.time.Instant;

import com.mealup.common.validation.Phones;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class VendorDtos {

    private VendorDtos() { }

    public record VendorResponse(Long id, String name, String contactPerson, String phoneNumber, String location,
                                 String notes, Long areaId, String areaName, Long logoVersion, int dailyLimit,
                                 boolean active, Instant createdAt, String mapUrl) { }

    /** Used to add and to edit. areaId is the neighbourhood (leave out for none). On edit, "active" switches the vendor on or off (leave it out to keep it as is). dailyLimit = pickups volunteers may book at this hotel per day (default 5). */
    public record VendorRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 120) String contactPerson,
            @NotBlank @Pattern(regexp = Phones.INPUT_PATTERN, message = "Enter a valid phone number") String phoneNumber,
            @NotBlank @Size(max = 500) String location,
            @Size(max = 2000) String notes,
            Long areaId,
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(500) Integer dailyLimit,
            Boolean active,
            @Size(max = 500) String mapUrl) { }   // null keeps the saved link, blank removes it
}

package com.mealup.vendors;

import java.time.Instant;

import com.mealup.common.validation.Phones;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class VendorDtos {

    private VendorDtos() { }

    public record VendorResponse(Long id, String name, String contactPerson, String phoneNumber, String location,
                                 String notes, boolean active, Instant createdAt) { }

    /** Used to add and to edit. On edit, "active" switches the vendor on or off (leave it out to keep it as is). */
    public record VendorRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 120) String contactPerson,
            @NotBlank @Pattern(regexp = Phones.INPUT_PATTERN, message = "Enter a valid phone number") String phoneNumber,
            @NotBlank @Size(max = 500) String location,
            @Size(max = 2000) String notes,
            Boolean active) { }
}

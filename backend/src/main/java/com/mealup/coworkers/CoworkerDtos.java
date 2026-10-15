package com.mealup.coworkers;

import java.time.Instant;

import com.mealup.common.validation.Phones;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class CoworkerDtos {

    private CoworkerDtos() { }

    public record CoworkerResponse(Long id, String name, String phoneNumber, String location, String notes,
                                   boolean active, Instant createdAt) { }

    /** Used to add and to edit. On edit, "active" switches the person on or off (leave it out to keep it as is). */
    public record CoworkerRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = Phones.INPUT_PATTERN, message = "Enter a valid phone number") String phoneNumber,
            @NotBlank @Size(max = 300) String location,
            @Size(max = 1000) String notes,
            Boolean active) { }
}

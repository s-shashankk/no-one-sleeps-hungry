package com.mealup.joins;

import java.time.Instant;

import com.mealup.common.validation.Phones;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class JoinDtos {

    private JoinDtos() { }

    public record JoinResponse(Long id, JoinStatus status, String name, String phoneNumber, String email,
                               String location, String message, Instant createdAt, Instant contactedAt) { }

    /** The public "join our community" form. */
    public record PublicJoinRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = Phones.INPUT_PATTERN, message = "Enter a valid phone number") String phone,
            @Size(max = 150) @Email(message = "Enter a valid email address") String email,
            @Size(max = 200) String location,
            @Size(max = 1000) String message,
            @AssertTrue(message = "Please agree so we can contact you.") boolean consent,
            String website) { }
}

package com.mealup.volunteers;

import java.time.Instant;

import com.mealup.users.Role;

import com.mealup.common.validation.Phones;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class VolunteerDtos {

    private VolunteerDtos() { }

    /** The public sign-up form. The password is stored only as a bcrypt hash. */
    public record RegisterRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email(message = "Enter a valid email address") @Size(max = 150) String email,
            @NotBlank @Pattern(regexp = Phones.INPUT_PATTERN, message = "Enter a valid phone number") String phone,
            @NotBlank @Size(min = 8, max = 72, message = "Use a password of 8 to 72 characters") String password,
            @AssertTrue(message = "Please agree so we can create your account.") boolean consent,
            String website) { }

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 200) String currentPassword,
            @NotBlank @Size(min = 8, max = 72, message = "Use a password of 8 to 72 characters") String newPassword) { }

    /** One volunteer with their totals, as the admin sees them. */
    public record VolunteerRow(Long id, String name, String email, String phone, boolean active,
                               Instant joinedAt, Instant lastLoginAt,
                               long pickups, long deliveries, long platesServed, long peopleReported,
                               Role role, Long hotelId, String hotelName) { }

    /** Gives hotel-staff access for one hotel. */
    public record MakeHotelRequest(@jakarta.validation.constraints.NotNull Long vendorId) { }

    /** A volunteer's own totals. deliveries = completed pickups; peopleReported = people they said they would feed. */
    public record MySummary(long pickups, long deliveries, long platesServed, long peopleReported, long inProgress) { }

    public record TemporaryPassword(String temporaryPassword) { }

    public record ActiveRequest(boolean active) { }
}

package com.mealup.requests;

import java.time.Instant;

import com.mealup.common.validation.Phones;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class RequestDtos {

    private RequestDtos() { }

    public record RequestResponse(Long id, RequestType type, RequestStatus status, String contactName,
                                  String contactPhone, String location, int quantity, String details,
                                  Integer mealsDelivered, String note,
                                  Instant createdAt, Instant updatedAt, Instant closedAt) { }

    /** The admin types in a request that arrived by phone or WhatsApp. */
    public record CreateRequest(
            @NotNull RequestType type,
            @Size(max = 120) String contactName,
            @Pattern(regexp = "^$|" + Phones.INPUT_PATTERN, message = "Enter a valid phone number") String contactPhone,
            @NotBlank @Size(max = 500) String location,
            @NotNull @Min(1) @Max(100000) Integer quantity,
            @Size(max = 1000) String details) { }

    /** Corrects the details of an existing request (its type and status do not change). */
    public record UpdateRequest(
            @Size(max = 120) String contactName,
            @Pattern(regexp = "^$|" + Phones.INPUT_PATTERN, message = "Enter a valid phone number") String contactPhone,
            @NotBlank @Size(max = 500) String location,
            @NotNull @Min(1) @Max(100000) Integer quantity,
            @Size(max = 1000) String details) { }

    public record CompleteRequest(@NotNull @Min(1) @Max(1000000) Integer mealsDelivered,
                                  @Size(max = 500) String note) { }

    /** Optional note for accepting or rejecting, e.g. why it could not be served. */
    public record NoteRequest(@Size(max = 500) String note) { }

    // ---- the two public forms -----------------------------------------------------------------

    /** "Tell us about people who need food". */
    public record PublicNeedFoodRequest(
            @Size(max = 120) String name,
            @Pattern(regexp = "^$|" + Phones.INPUT_PATTERN, message = "Enter a valid phone number") String phone,
            @NotBlank @Size(max = 500) String location,
            @NotNull @Min(1) @Max(10000) Integer people,
            @Size(max = 1000) String details,
            @AssertTrue(message = "Please agree so we can use these details to arrange help.") boolean consent,
            String website) { }

    /** "Tell us about surplus food". The phone number is required so the admin can call back. */
    public record PublicSurplusRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Pattern(regexp = Phones.INPUT_PATTERN, message = "Enter a valid phone number") String phone,
            @NotBlank @Size(max = 500) String location,
            @NotNull @Min(1) @Max(100000) Integer meals,
            @Size(max = 1000) String details,
            @AssertTrue(message = "Please agree so we can use these details to arrange the pickup.") boolean consent,
            String website) { }

    public record PublicSubmissionResponse(String message) { }
}

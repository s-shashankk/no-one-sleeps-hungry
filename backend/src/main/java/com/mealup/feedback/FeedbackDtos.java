package com.mealup.feedback;

import java.time.Instant;

import com.mealup.users.Role;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class FeedbackDtos {

    private FeedbackDtos() { }

    /** rating (1 to 5 stars) is optional and only for feedback. */
    public record SendRequest(
            @NotNull FeedbackKind kind,
            @Min(1) @Max(5) Integer rating,
            @NotBlank @Size(max = 2000) String body) { }

    /** What the sender sees about their own notes. */
    public record MyMessage(Long id, FeedbackKind kind, Integer rating, String body, FeedbackStatus status,
                            Instant createdAt) { }

    /** What the admin sees, with who sent it. */
    public record AdminMessage(Long id, FeedbackKind kind, Integer rating, String body, FeedbackStatus status,
                               Long senderId, String senderName, String senderEmail, String senderPhone,
                               Role senderRole, String hotelName, Instant createdAt, Instant handledAt) { }
}

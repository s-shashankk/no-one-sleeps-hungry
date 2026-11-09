package com.mealup.feedback;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.mealup.users.User;

import jakarta.persistence.*;

/** A note from a volunteer or hotel staff member to the admin: feedback (with an optional star rating) or a plain message. */
@Entity
@Table(name = "feedback_messages")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private FeedbackKind kind;

    private Integer rating;

    private String body;

    @Enumerated(EnumType.STRING)
    private FeedbackStatus status = FeedbackStatus.NEW;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    protected Feedback() { }

    public Feedback(User user, FeedbackKind kind, Integer rating, String body) {
        this.user = user;
        this.kind = kind;
        this.rating = rating;
        this.body = body;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void markDone() {
        status = FeedbackStatus.DONE;
        handledAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void reopen() {
        status = FeedbackStatus.NEW;
        handledAt = null;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public FeedbackKind getKind() { return kind; }
    public Integer getRating() { return rating; }
    public String getBody() { return body; }
    public FeedbackStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getHandledAt() { return handledAt; }
}

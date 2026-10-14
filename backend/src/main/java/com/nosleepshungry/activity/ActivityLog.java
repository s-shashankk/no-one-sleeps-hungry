package com.nosleepshungry.activity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "activity_log")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actor;
    private String entity;
    private Long entityId;
    private String action;
    private String details;
    private LocalDateTime createdAt = LocalDateTime.now();

    protected ActivityLog() { }

    public ActivityLog(String actor, String entity, Long entityId, String action, String details) {
        this.actor = actor;
        this.entity = entity;
        this.entityId = entityId;
        this.action = action;
        this.details = details == null ? null : (details.length() > 500 ? details.substring(0, 500) : details);
    }
}

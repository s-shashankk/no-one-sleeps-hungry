package com.nosleepshungry.activity;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Records who did what. Call it from inside the same transaction as the change. */
@Service
public class ActivityService {

    private final ActivityLogRepository repo;

    public ActivityService(ActivityLogRepository repo) {
        this.repo = repo;
    }

    public void log(String entity, Long entityId, String action, String details) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        String actor = (a == null || a.getName() == null) ? "system" : a.getName();
        repo.save(new ActivityLog(actor, entity, entityId, action, details));
    }
}

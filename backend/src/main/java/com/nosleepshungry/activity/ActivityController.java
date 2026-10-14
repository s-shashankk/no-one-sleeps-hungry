package com.nosleepshungry.activity;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Super Admin only (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin/activity")
public class ActivityController {

    private final ActivityLogRepository repo;

    public ActivityController(ActivityLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ActivityLog> latest() {
        return repo.findTop200ByOrderByCreatedAtDesc();
    }
}

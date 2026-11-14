package com.mealup.dashboard;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mealup.access.AccessPolicy;
import com.mealup.dashboard.DashboardDtos.Summary;
import com.mealup.publicapi.PublicSnapshotCache;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    /** When the public pages (numbers, areas, partner hotels and logos) were last rebuilt. */
    public record PublicRefresh(Instant builtAt) { }

    private final DashboardService service;
    private final PublicSnapshotCache publicCache;
    private final AccessPolicy access;

    public DashboardController(DashboardService service, PublicSnapshotCache publicCache, AccessPolicy access) {
        this.service = service;
        this.publicCache = publicCache;
        this.access = access;
    }

    /** Admin button: rebuild everything the home page shows (numbers, areas, partner hotels, logos) right now. */
    @PostMapping("/refresh-public")
    public PublicRefresh refreshPublic() {
        access.requireUser();
        publicCache.refresh();
        return new PublicRefresh(publicCache.builtAt());
    }

    @GetMapping
    public Summary summary() {
        return service.summary();
    }
}

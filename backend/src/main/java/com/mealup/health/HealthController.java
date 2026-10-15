package com.mealup.health;

import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health")
public class HealthController {

    public record HealthResponse(String status, String database, Instant time) { }

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    @Operation(summary = "Is the API (and its database) running?")
    public HealthResponse health() {
        String database;
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            database = "UP";
        } catch (Exception e) {
            database = "DOWN";
        }
        return new HealthResponse("UP".equals(database) ? "UP" : "DEGRADED", database, Instant.now());
    }
}

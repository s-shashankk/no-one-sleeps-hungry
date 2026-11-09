package com.mealup.texts;

import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;

/** The website reads the edited wording without signing in; only the admin can change it. */
@RestController
@Tag(name = "Site text")
public class SiteTextController {

    public record TextsRequest(@NotNull Map<String, String> values) { }

    private final SiteTextService service;

    public SiteTextController(SiteTextService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/public/texts")
    public ResponseEntity<Map<String, String>> edited() {
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(new java.util.TreeMap<>(service.forVisitors()));
    }

    @PutMapping("/api/v1/texts")
    public Map<String, String> save(@RequestBody @jakarta.validation.Valid TextsRequest req) {
        return service.save(req.values());
    }
}

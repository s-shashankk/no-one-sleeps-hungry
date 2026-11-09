package com.mealup.publicapi;

import java.util.List;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mealup.joins.JoinDtos.PublicJoinRequest;
import com.mealup.joins.JoinService;
import com.mealup.publicapi.PublicDtos.Impact;
import com.mealup.publicapi.PublicDtos.PublicArea;
import com.mealup.publicapi.PublicDtos.PublicHotel;
import com.mealup.publicapi.PublicDtos.PublicMessage;
import com.mealup.publicapi.PublicDtos.PublicPartner;
import com.mealup.vendors.LogoStore;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * No sign-in. Reads show only counts and hotel names/addresses. The only write is a join request,
 * which only the signed-in admin sees.
 */
@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Public site")
public class PublicNetworkController {

    private final ImpactService impact;
    private final PublicSnapshotCache cache;
    private final JoinService joins;

    public PublicNetworkController(ImpactService impact, PublicSnapshotCache cache, JoinService joins) {
        this.impact = impact;
        this.cache = cache;
        this.joins = joins;
    }

    @GetMapping("/impact")
    public Impact impact() {
        return cache.impact();
    }

    @GetMapping("/areas")
    public List<PublicArea> areas() {
        return cache.areas();
    }

    @GetMapping("/areas/{areaId}/hotels")
    public List<PublicHotel> hotels(@PathVariable Long areaId) {
        return impact.hotels(areaId);
    }

    /** Partner hotels for the home-page slider. */
    @GetMapping("/partners")
    public List<PublicPartner> partners() {
        return cache.partners();
    }

    /** A partner hotel's logo. The URL carries ?v=<logoVersion>, so it can be cached for a long time. */
    @GetMapping("/hotels/{id}/logo")
    public ResponseEntity<byte[]> logo(@PathVariable Long id) {
        LogoStore.Logo logo = impact.logo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(logo.data());
    }

    @PostMapping("/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicMessage join(@Valid @RequestBody PublicJoinRequest body) {
        return joins.submit(body);
    }
}

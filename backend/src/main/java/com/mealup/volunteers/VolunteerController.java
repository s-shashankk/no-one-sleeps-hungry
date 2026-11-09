package com.mealup.volunteers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mealup.common.api.PageResponse;
import com.mealup.common.api.Paging;
import com.mealup.pickups.PickupDtos.PickupResponse;
import com.mealup.volunteers.VolunteerDtos.TemporaryPassword;
import com.mealup.volunteers.VolunteerDtos.VolunteerRow;

import io.swagger.v3.oas.annotations.tags.Tag;

/** Admin: every volunteer who signed up, with how many times they delivered and how many plates they served. */
@RestController
@RequestMapping("/api/v1/volunteers")
@Tag(name = "Volunteers (admin)")
public class VolunteerController {

    private final VolunteerAdminService service;

    public VolunteerController(VolunteerAdminService service) {
        this.service = service;
    }

    /** sort = recent (default) | deliveries | plates | name */
    @GetMapping
    public PageResponse<VolunteerRow> list(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) Boolean active,
                                           @RequestParam(required = false, defaultValue = "recent") String sort,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size) {
        int p = page == null ? 0 : Math.max(page, 0);
        int s = size == null ? Paging.DEFAULT_SIZE : Math.min(Math.max(size, 1), Paging.MAX_SIZE);
        return service.list(q, active, sort, p, s);
    }

    /** A spreadsheet of all volunteers (opens in Excel or Google Sheets). */
    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> export() {
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("Name,Email,Phone,Joined,Last sign-in,Account active,Type,Hotel,Pickups,Deliveries done,Plates served,People reported\r\n");
        List<VolunteerRow> rows = service.all();
        for (VolunteerRow r : rows) {
            csv.append(cell(r.name())).append(',').append(cell(r.email())).append(',').append(cell(r.phone())).append(',')
                    .append(cell(date(r.joinedAt()))).append(',').append(cell(date(r.lastLoginAt()))).append(',')
                    .append(r.active() ? "yes" : "no").append(',').append(r.role() == com.mealup.users.Role.HOTEL ? "hotel staff" : "volunteer").append(',').append(cell(r.hotelName())).append(',').append(r.pickups()).append(',').append(r.deliveries())
                    .append(',').append(r.platesServed()).append(',').append(r.peopleReported()).append("\r\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mealup-volunteers.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/{id}/pickups")
    public PageResponse<PickupResponse> pickups(@PathVariable Long id,
                                                @RequestParam(required = false) Integer page,
                                                @RequestParam(required = false) Integer size) {
        return PageResponse.of(service.pickupsOf(id, Paging.of(page, size, Sort.by("createdAt", "id").descending())));
    }

    @PostMapping("/{id}/enable")
    public void enable(@PathVariable Long id) {
        service.setActive(id, true);
    }

    @PostMapping("/{id}/disable")
    public void disable(@PathVariable Long id) {
        service.setActive(id, false);
    }

    /** Gives this person hotel-staff access for one hotel (they then see and confirm that hotel's pickups). */
    @PostMapping("/{id}/make-hotel")
    public void makeHotel(@PathVariable Long id, @jakarta.validation.Valid @RequestBody VolunteerDtos.MakeHotelRequest body) {
        service.makeHotel(id, body.vendorId());
    }

    @PostMapping("/{id}/make-volunteer")
    public void makeVolunteer(@PathVariable Long id) {
        service.makeVolunteer(id);
    }

    /** Returns a new temporary password, once. Give it to the volunteer; they can change it after signing in. */
    @PostMapping("/{id}/reset-password")
    public TemporaryPassword resetPassword(@PathVariable Long id) {
        return service.resetPassword(id);
    }

    private static String date(Instant i) {
        return i == null ? "" : i.toString();
    }

    /** One CSV cell. Quotes are doubled, and a leading = + - @ is neutralised so a spreadsheet never runs it as a formula. */
    private static String cell(String value) {
        String v = value == null ? "" : value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}

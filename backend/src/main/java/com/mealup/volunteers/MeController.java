package com.mealup.volunteers;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.mealup.common.api.PageResponse;
import com.mealup.common.api.Paging;
import com.mealup.pickups.PickupDtos.PledgeRequest;
import com.mealup.pickups.PickupDtos.VolunteerPickupResponse;
import com.mealup.pickups.PickupService;
import com.mealup.volunteers.VolunteerDtos.ChangePasswordRequest;
import com.mealup.volunteers.VolunteerDtos.MySummary;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** A signed-in volunteer: their totals, their pickups, a new pickup, and their password. */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "My deliveries")
public class MeController {

    private final VolunteerAccountService accounts;
    private final PickupService pickups;

    public MeController(VolunteerAccountService accounts, PickupService pickups) {
        this.accounts = accounts;
        this.pickups = pickups;
    }

    @GetMapping("/summary")
    public MySummary summary() {
        return accounts.mySummary();
    }

    @GetMapping("/pickups")
    public PageResponse<VolunteerPickupResponse> myPickups(@RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size) {
        return PageResponse.of(pickups.myPickups(Paging.of(page, size, Sort.by("createdAt", "id").descending())));
    }

    /** "I will collect food from this hotel for about this many people." */
    @PostMapping("/pickups")
    @ResponseStatus(HttpStatus.CREATED)
    public VolunteerPickupResponse pledge(@Valid @RequestBody PledgeRequest body) {
        return pickups.pledge(body);
    }

    @PostMapping("/pickups/{id}/cancel")
    public VolunteerPickupResponse cancel(@PathVariable Long id) {
        return pickups.cancelMine(id);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest body) {
        accounts.changePassword(body);
    }
}

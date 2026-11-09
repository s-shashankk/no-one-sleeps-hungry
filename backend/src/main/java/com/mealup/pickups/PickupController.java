package com.mealup.pickups;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import com.mealup.common.api.PageResponse;
import com.mealup.common.api.Paging;
import com.mealup.pickups.PickupDtos.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/pickups")
@Tag(name = "Pickups")
public class PickupController {

    private final PickupService service;

    public PickupController(PickupService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<PickupResponse> list(@RequestParam(required = false) PickupStatus status,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size) {
        return PageResponse.of(service.list(status, Paging.of(page, size, Sort.by("createdAt", "id").descending())));
    }

    /** The admin has informed the hotel. */
    @PostMapping("/{id}/confirm")
    public PickupResponse confirm(@PathVariable Long id) {
        return service.confirm(id);
    }

    /** Food collected and served: records the plates. */
    @PostMapping("/{id}/complete")
    public PickupResponse complete(@PathVariable Long id, @Valid @RequestBody CompletePickupRequest body) {
        return service.complete(id, body);
    }

    /** Accept on the hotel's behalf: the meal counts as served (one plate per person). */
    @PostMapping("/{id}/accept")
    public PickupResponse accept(@PathVariable Long id) {
        return service.accept(id);
    }

    @PostMapping("/{id}/cancel")
    public PickupResponse cancel(@PathVariable Long id, @Valid @RequestBody PickupNoteRequest body) {
        return service.cancel(id, body);
    }

    @PostMapping("/{id}/reopen")
    public PickupResponse reopen(@PathVariable Long id) {
        return service.reopen(id);
    }
}

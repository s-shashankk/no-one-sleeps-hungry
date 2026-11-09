package com.mealup.hotel;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import com.mealup.common.api.PageResponse;
import com.mealup.common.api.Paging;
import com.mealup.hotel.HotelDtos.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Hotel staff: their own hotel's bookings. */
@RestController
@RequestMapping("/api/v1/hotel")
@Tag(name = "Hotel staff")
public class HotelController {

    private final HotelService service;

    public HotelController(HotelService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public HotelSummary summary() {
        return service.summary();
    }

    /** open=true (default) shows only people still to come; open=false shows the whole history. */
    @GetMapping("/pickups")
    public PageResponse<HotelPickup> list(@RequestParam(required = false, defaultValue = "true") boolean open,
                                          @RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer size) {
        Sort sort = open ? Sort.by("createdAt", "id") : Sort.by("createdAt", "id").descending();
        return PageResponse.of(service.list(open, Paging.of(page, size, sort)));
    }

    /** Accept the booking: food is handed over, and it counts as served. */
    @PostMapping("/pickups/{id}/accept")
    public HotelPickup accept(@PathVariable Long id) {
        return service.accept(id);
    }

    @PostMapping("/pickups/{id}/decline")
    public HotelPickup decline(@PathVariable Long id, @Valid @RequestBody DeclineRequest body) {
        return service.decline(id, body);
    }
}

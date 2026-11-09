package com.mealup.joins;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import com.mealup.common.api.PageResponse;
import com.mealup.common.api.Paging;
import com.mealup.joins.JoinDtos.JoinResponse;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/join-requests")
@Tag(name = "Join requests")
public class JoinController {

    private final JoinService service;

    public JoinController(JoinService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<JoinResponse> list(@RequestParam(required = false) JoinStatus status,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size) {
        return PageResponse.of(service.list(status, Paging.of(page, size, Sort.by("createdAt", "id").descending())));
    }

    @PostMapping("/{id}/contacted")
    public JoinResponse contacted(@PathVariable Long id) {
        return service.markContacted(id);
    }

    @PostMapping("/{id}/reopen")
    public JoinResponse reopen(@PathVariable Long id) {
        return service.reopen(id);
    }
}

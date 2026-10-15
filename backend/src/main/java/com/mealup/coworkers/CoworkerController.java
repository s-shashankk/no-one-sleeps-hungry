package com.mealup.coworkers;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.mealup.common.api.PageResponse;
import com.mealup.common.api.Paging;
import com.mealup.coworkers.CoworkerDtos.CoworkerRequest;
import com.mealup.coworkers.CoworkerDtos.CoworkerResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/coworkers")
@Tag(name = "Coworkers")
public class CoworkerController {

    private final CoworkerService service;

    public CoworkerController(CoworkerService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CoworkerResponse> list(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) Boolean active,
                                               @RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size) {
        return PageResponse.of(service.list(q, active, Paging.of(page, size, Sort.by("name"))));
    }

    @GetMapping("/{id}")
    public CoworkerResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CoworkerResponse create(@Valid @RequestBody CoworkerRequest body) {
        return service.create(body);
    }

    @PutMapping("/{id}")
    public CoworkerResponse update(@PathVariable Long id, @Valid @RequestBody CoworkerRequest body) {
        return service.update(id, body);
    }
}

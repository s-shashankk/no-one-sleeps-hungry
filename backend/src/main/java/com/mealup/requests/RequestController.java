package com.mealup.requests;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.mealup.common.api.PageResponse;
import com.mealup.common.api.Paging;
import com.mealup.requests.RequestDtos.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/requests")
@Tag(name = "Requests")
public class RequestController {

    private final RequestService service;

    public RequestController(RequestService service) {
        this.service = service;
    }

    /** sort = newest (default) | oldest (handy for working through the pending list). */
    @GetMapping
    public PageResponse<RequestResponse> list(@RequestParam(required = false) RequestType type,
                                              @RequestParam(required = false) RequestStatus status,
                                              @RequestParam(required = false) String q,
                                              @RequestParam(required = false, defaultValue = "newest") String sort,
                                              @RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size) {
        Sort order = "oldest".equals(sort) ? Sort.by("createdAt", "id") : Sort.by("createdAt", "id").descending();
        return PageResponse.of(service.list(type, status, q, Paging.of(page, size, order)));
    }

    @GetMapping("/{id}")
    public RequestResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResponse create(@Valid @RequestBody CreateRequest body) {
        return service.create(body);
    }

    @PutMapping("/{id}")
    public RequestResponse update(@PathVariable Long id, @Valid @RequestBody UpdateRequest body) {
        return service.update(id, body);
    }

    /** Food delivered: records the number of meals and closes the request. */
    @PostMapping("/{id}/complete")
    public RequestResponse complete(@PathVariable Long id, @Valid @RequestBody CompleteRequest body) {
        return service.complete(id, body);
    }

    /** Surplus food accepted: a team will be sent to collect it. */
    @PostMapping("/{id}/accept")
    public RequestResponse accept(@PathVariable Long id, @Valid @RequestBody NoteRequest body) {
        return service.accept(id, body);
    }

    /** Unable to serve / unable to collect: the request goes off the pending list. */
    @PostMapping("/{id}/reject")
    public RequestResponse reject(@PathVariable Long id, @Valid @RequestBody NoteRequest body) {
        return service.reject(id, body);
    }

    @PostMapping("/{id}/reopen")
    public RequestResponse reopen(@PathVariable Long id) {
        return service.reopen(id);
    }
}

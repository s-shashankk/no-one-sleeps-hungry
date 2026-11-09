package com.mealup.feedback;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.mealup.common.api.PageResponse;
import com.mealup.common.api.Paging;
import com.mealup.feedback.FeedbackDtos.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Volunteers and hotel staff: write to the admin, and see what they sent. */
@RestController
@RequestMapping("/api/v1/account/messages")
@Tag(name = "Messages to admin")
public class FeedbackController {

    private final FeedbackService service;

    public FeedbackController(FeedbackService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MyMessage send(@Valid @RequestBody SendRequest body) {
        return service.send(body);
    }

    @GetMapping
    public PageResponse<MyMessage> mine(@RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer size) {
        return PageResponse.of(service.myMessages(Paging.of(page, size, Sort.by("createdAt", "id").descending())));
    }
}

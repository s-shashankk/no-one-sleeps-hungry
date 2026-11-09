package com.mealup.feedback;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import com.mealup.common.api.PageResponse;
import com.mealup.common.api.Paging;
import com.mealup.feedback.FeedbackDtos.AdminMessage;

import io.swagger.v3.oas.annotations.tags.Tag;

/** Admin: read feedback and messages, and mark them done. */
@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "Messages (admin)")
public class AdminFeedbackController {

    private final FeedbackService service;

    public AdminFeedbackController(FeedbackService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AdminMessage> list(@RequestParam(required = false) FeedbackStatus status,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size) {
        return PageResponse.of(service.list(status, Paging.of(page, size, Sort.by("createdAt", "id").descending())));
    }

    @PostMapping("/{id}/done")
    public AdminMessage done(@PathVariable Long id) {
        return service.done(id);
    }

    @PostMapping("/{id}/reopen")
    public AdminMessage reopen(@PathVariable Long id) {
        return service.reopen(id);
    }
}

package com.mealup.requests;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.mealup.requests.RequestDtos.PublicNeedFoodRequest;
import com.mealup.requests.RequestDtos.PublicSubmissionResponse;
import com.mealup.requests.RequestDtos.PublicSurplusRequest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * No sign-in. Everything sent here becomes a pending request that only a signed-in admin can see;
 * nothing sent here is ever shown publicly.
 */
@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Public forms")
public class PublicController {

    private final RequestService service;

    public PublicController(RequestService service) {
        this.service = service;
    }

    @PostMapping("/hunger-reports")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicSubmissionResponse needFood(@Valid @RequestBody PublicNeedFoodRequest body) {
        return service.submitNeedFood(body);
    }

    @PostMapping("/surplus-food-reports")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicSubmissionResponse surplusFood(@Valid @RequestBody PublicSurplusRequest body) {
        return service.submitSurplus(body);
    }
}

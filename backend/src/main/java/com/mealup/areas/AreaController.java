package com.mealup.areas;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.mealup.areas.AreaDtos.AreaRequest;
import com.mealup.areas.AreaDtos.AreaResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/areas")
@Tag(name = "Areas")
public class AreaController {

    private final AreaService service;

    public AreaController(AreaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AreaResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AreaResponse create(@Valid @RequestBody AreaRequest body) {
        return service.create(body);
    }

    @PutMapping("/{id}")
    public AreaResponse update(@PathVariable Long id, @Valid @RequestBody AreaRequest body) {
        return service.update(id, body);
    }
}

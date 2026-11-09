package com.mealup.vendors;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.mealup.common.api.PageResponse;
import com.mealup.common.api.Paging;
import com.mealup.vendors.VendorDtos.VendorRequest;
import com.mealup.vendors.VendorDtos.VendorResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/vendors")
@Tag(name = "Vendors")
public class VendorController {

    private final VendorService service;

    public VendorController(VendorService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<VendorResponse> list(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) Boolean active,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size) {
        return PageResponse.of(service.list(q, active, Paging.of(page, size, Sort.by("name"))));
    }

    @GetMapping("/{id}")
    public VendorResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendorResponse create(@Valid @RequestBody VendorRequest body) {
        return service.create(body);
    }

    @PutMapping("/{id}")
    public VendorResponse update(@PathVariable Long id, @Valid @RequestBody VendorRequest body) {
        return service.update(id, body);
    }

    /** Upload the hotel's logo (form field "file"; PNG, JPG or WebP, up to 500 KB). */
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VendorResponse uploadLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return service.setLogo(id, file);
    }

    @DeleteMapping("/{id}/logo")
    public VendorResponse deleteLogo(@PathVariable Long id) {
        return service.removeLogo(id);
    }
}

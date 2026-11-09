package com.mealup.areas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AreaDtos {

    private AreaDtos() { }

    public record AreaResponse(Long id, String city, String name, boolean open) { }

    public record AreaRequest(
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 120) String name,
            boolean open) { }
}

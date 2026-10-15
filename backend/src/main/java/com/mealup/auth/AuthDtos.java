package com.mealup.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() { }

    public record LoginRequest(@NotBlank @Size(max = 150) String email,
                               @NotBlank @Size(max = 200) String password) { }

    /** The signed-in admin, as the frontend needs it. */
    public record MeResponse(Long id, String fullName, String email, String phoneNumber) { }

    public record CsrfResponse(String headerName, String token) { }
}

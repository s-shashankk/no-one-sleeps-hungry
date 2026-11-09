package com.mealup.auth;

import com.mealup.users.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() { }

    public record LoginRequest(@NotBlank @Size(max = 150) String email,
                               @NotBlank @Size(max = 200) String password) { }

    /** The signed-in person (admin or volunteer), as the frontend needs it. */
    public record MeResponse(Long id, String fullName, String email, String phoneNumber, Role role) { }

    public record CsrfResponse(String headerName, String token) { }
}

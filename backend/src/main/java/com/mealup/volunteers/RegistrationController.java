package com.mealup.volunteers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mealup.access.AuthenticatedUser;
import com.mealup.auth.AuthDtos.MeResponse;
import com.mealup.auth.AuthService;
import com.mealup.auth.SessionLogin;
import com.mealup.volunteers.VolunteerDtos.RegisterRequest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/** Public sign-up for volunteers. The new person is signed in straight away. */
@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Volunteer sign-up")
public class RegistrationController {

    private final VolunteerAccountService accounts;
    private final AuthService authService;
    private final SessionLogin sessionLogin;

    public RegistrationController(VolunteerAccountService accounts, AuthService authService, SessionLogin sessionLogin) {
        this.accounts = accounts;
        this.authService = authService;
        this.sessionLogin = sessionLogin;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MeResponse register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request,
                               HttpServletResponse response) {
        AuthenticatedUser user = accounts.register(body);
        sessionLogin.start(user, request, response);
        return authService.me(user);
    }
}

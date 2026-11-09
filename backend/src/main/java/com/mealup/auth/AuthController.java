package com.mealup.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import com.mealup.access.AccessPolicy;
import com.mealup.access.AuthenticatedUser;
import com.mealup.auth.AuthDtos.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/** Login with a server-side session in an HttpOnly cookie. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final SessionLogin sessionLogin;
    private final AccessPolicy access;

    public AuthController(AuthService authService, SessionLogin sessionLogin, AccessPolicy access) {
        this.authService = authService;
        this.sessionLogin = sessionLogin;
        this.access = access;
    }

    /** The frontend calls this once; it also makes the server set the readable XSRF-TOKEN cookie. */
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/login")
    public MeResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
                            HttpServletResponse response) {
        AuthenticatedUser user = authService.login(body.email(), body.password());

        sessionLogin.start(user, request, response);

        return authService.me(user);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    @GetMapping("/me")
    public MeResponse me() {
        return authService.me(access.user());
    }
}

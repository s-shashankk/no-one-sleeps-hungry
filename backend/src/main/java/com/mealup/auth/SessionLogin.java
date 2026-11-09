package com.mealup.auth;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import com.mealup.access.AuthenticatedUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Starts the login session after a successful sign-in or sign-up. */
@Component
public class SessionLogin {

    private final SecurityContextRepository contextRepository;

    public SessionLogin(SecurityContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    public void start(AuthenticatedUser user, HttpServletRequest request, HttpServletResponse response) {
        // A new session id after login prevents session fixation.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities()));
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}

package com.mealup.auth;

import java.io.IOException;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mealup.access.AuthenticatedUser;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Runs on every request that carries a login session and re-checks the person (from a short-lived memory copy, see UserCheckCache).
 * A disabled (or deleted) account loses access on the very next request. Not a Spring bean on purpose: SecurityConfig adds it to the chain.
 */
public class SessionUserRefreshFilter extends OncePerRequestFilter {

    private final UserCheckCache users;

    public SessionUserRefreshFilter(UserCheckCache users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication auth = context.getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser sessionUser) {
            Optional<AuthenticatedUser> current = users.lookup(sessionUser.id());
            if (current.isEmpty()) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            } else {
                AuthenticatedUser fresh = current.get();
                context.setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(fresh, null, fresh.authorities()));
            }
        }
        chain.doFilter(request, response);
    }
}

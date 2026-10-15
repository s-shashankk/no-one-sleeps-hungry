package com.mealup.access;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.mealup.common.exception.UnauthorizedException;

/**
 * Every signed-in person is an admin with the same rights. Services still ask this class first, so the
 * rule holds even if a URL rule is ever loosened by mistake.
 */
@Component
public class AccessPolicy {

    public AuthenticatedUser user() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser u) {
            return u;
        }
        throw new UnauthorizedException("Authentication is required.");
    }

    /** Throws 401 unless an admin is signed in. */
    public void requireUser() {
        user();
    }
}

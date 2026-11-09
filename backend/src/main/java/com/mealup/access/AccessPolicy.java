package com.mealup.access;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.mealup.common.exception.UnauthorizedException;
import com.mealup.users.Role;

/**
 * Two kinds of signed-in people: admins (everything in the admin pages) and volunteers (only their own account
 * and deliveries). Services ask this class first, so the rule holds even if a URL rule is ever loosened by mistake.
 */
@Component
public class AccessPolicy {

    /** Whoever is signed in (admin or volunteer). Throws 401 when nobody is. */
    public AuthenticatedUser user() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser u) {
            return u;
        }
        throw new UnauthorizedException("Authentication is required.");
    }

    /** Throws 401 when nobody is signed in and 403 when a volunteer (not an admin) is. */
    public AuthenticatedUser requireUser() {
        AuthenticatedUser u = user();
        if (u.role() != Role.ADMIN) {
            throw new AccessDeniedException("Admins only.");
        }
        return u;
    }

    /** Throws 401 when nobody is signed in and 403 unless the person is hotel staff. */
    public AuthenticatedUser requireHotel() {
        AuthenticatedUser u = user();
        if (u.role() != Role.HOTEL) {
            throw new AccessDeniedException("Hotel staff only.");
        }
        return u;
    }

    /** Volunteers and hotel staff (who both own an account with a password they can change). */
    public AuthenticatedUser requireAccountHolder() {
        AuthenticatedUser u = user();
        if (u.role() != Role.VOLUNTEER && u.role() != Role.HOTEL) {
            throw new AccessDeniedException("Admins change their password elsewhere.");
        }
        return u;
    }

    /** Throws 401 when nobody is signed in and 403 when an admin (not a volunteer) is. */
    public AuthenticatedUser requireVolunteer() {
        AuthenticatedUser u = user();
        if (u.role() != Role.VOLUNTEER) {
            throw new AccessDeniedException("Please sign in with a volunteer account.");
        }
        return u;
    }
}

package com.nosleepshungry.security;

import java.util.HashSet;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.nosleepshungry.model.User;
import com.nosleepshungry.repo.UserRepository;

/**
 * Who is calling, and which localities may they touch?
 * Looked up from the database on every request, so suspending a coordinator
 * takes effect immediately even if their login token is still valid.
 */
@Service
public class AccessService {

    public record Scope(boolean all, Set<Long> localityIds) {
        public boolean allows(Long localityId) {
            return all || (localityId != null && localityIds.contains(localityId));
        }
    }

    private final UserRepository users;

    public AccessService(UserRepository users) {
        this.users = users;
    }

    public User currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
        }
        return users.findByEmailIgnoreCase(a.getName())
                .filter(u -> u.getStatus() == User.Status.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again."));
    }

    public Scope scope() {
        User u = currentUser();
        return new Scope(u.getRole() == User.Role.SUPER_ADMIN, new HashSet<>(u.getLocalityIds()));
    }

    public void requireSuperAdmin() {
        if (!scope().all()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Super Admin can do this.");
        }
    }
}

package com.mealup.auth;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mealup.access.AuthenticatedUser;
import com.mealup.users.User;
import com.mealup.users.UserRepository;

/**
 * Remembers "this signed-in person exists, is active and has this role" for a short time, so every request
 * does not ask the database again. When an admin disables someone or changes their role, that person is
 * dropped from here at once (VolunteerAdminService); any other change is picked up within the time below.
 * Set mealup.cache.user-check-seconds=0 to check the database on every request (the tests do this).
 */
@Component
public class UserCheckCache {

    private static final int MAX_ENTRIES = 5_000;

    private record Entry(Optional<AuthenticatedUser> user, long loadedAt) { }

    private final UserRepository users;
    private final long keepMillis;
    private final Map<Long, Entry> entries = new ConcurrentHashMap<>();

    public UserCheckCache(UserRepository users, @Value("${mealup.cache.user-check-seconds:45}") long seconds) {
        this.users = users;
        this.keepMillis = Math.max(0, seconds) * 1000L;
    }

    /** The signed-in person as they are now, or empty if the account is gone or disabled. */
    public Optional<AuthenticatedUser> lookup(Long id) {
        long now = System.currentTimeMillis();
        if (keepMillis > 0) {
            Entry hit = entries.get(id);
            if (hit != null && now - hit.loadedAt() < keepMillis) return hit.user();
        }
        Optional<AuthenticatedUser> fresh = users.findById(id).filter(User::isActive).map(AuthenticatedUser::from);
        if (keepMillis > 0) {
            if (entries.size() >= MAX_ENTRIES) entries.clear();   // keeps memory bounded
            entries.put(id, new Entry(fresh, now));
        }
        return fresh;
    }

    /** Forget one person so the next request checks the database. */
    public void evict(Long id) {
        entries.remove(id);
    }
}

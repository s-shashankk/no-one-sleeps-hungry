package com.nosleepshungry.web;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nosleepshungry.model.User;
import com.nosleepshungry.repo.UserRepository;
import com.nosleepshungry.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int MAX_FAILURES = 10;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    public record LoginRequest(@NotBlank String email, @NotBlank String password) { }

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    // very simple brute-force guard: recent failed logins per client address
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest r, HttpServletRequest http) {
        String client = http.getRemoteAddr();
        if (recentFailures(client) >= MAX_FAILURES) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed attempts. Try again in a few minutes.");
        }
        User user = users.findByEmailIgnoreCase(r.email().trim())
                .filter(u -> u.getStatus() == User.Status.ACTIVE)
                .filter(u -> encoder.matches(r.password(), u.getPasswordHash()))
                .orElse(null);
        if (user == null) {
            failures.computeIfAbsent(client, k -> new ArrayDeque<>()).addLast(Instant.now());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password.");
        }
        failures.remove(client);
        return Map.of("token", jwt.create(user), "name", user.getName(), "role", user.getRole().name());
    }

    private int recentFailures(String client) {
        Deque<Instant> q = failures.get(client);
        if (q == null) return 0;
        synchronized (q) {
            Instant cutoff = Instant.now().minus(WINDOW);
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.pollFirst();
            return q.size();
        }
    }
}

package com.mealup.security;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * A small sliding-window limiter kept in memory: "at most N tries per key in the last window".
 * It protects the public forms and the login from floods. It is per server process, which is right for
 * the single-server setup this project uses; a multi-server setup would need a shared store.
 */
@Component
public class RateLimiter {

    private static final int CLEAN_WHEN_LARGER_THAN = 10_000;

    private final Clock clock;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimiter() {
        this(Clock.systemUTC());
    }

    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Counts one try for the key and says whether it is allowed.
     * @return 0 when allowed; otherwise the number of seconds until a try would be allowed again
     */
    public long tryAcquire(String key, int limit, Duration window) {
        long now = clock.millis();
        long windowMs = window.toMillis();
        if (hits.size() > CLEAN_WHEN_LARGER_THAN) {
            hits.values().removeIf(q -> synchronizedPrune(q, now, windowMs));
        }
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() >= windowMs) {
                q.pollFirst();
            }
            if (q.size() >= limit) {
                long wait = windowMs - (now - q.peekFirst());
                return Math.max(1, (wait + 999) / 1000);
            }
            q.addLast(now);
            return 0;
        }
    }

    private static boolean synchronizedPrune(Deque<Long> q, long now, long windowMs) {
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() >= windowMs) {
                q.pollFirst();
            }
            return q.isEmpty();
        }
    }
}

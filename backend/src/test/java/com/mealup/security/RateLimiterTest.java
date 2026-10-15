package com.mealup.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class RateLimiterTest {

    /** A clock the test can move forward. */
    static class TestClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void allowsUpToTheLimitThenAsksToWaitAndRecoversLater() {
        TestClock clock = new TestClock();
        RateLimiter limiter = new RateLimiter(clock);
        Duration window = Duration.ofMinutes(10);

        for (int i = 0; i < 3; i++) assertThat(limiter.tryAcquire("a", 3, window)).isZero();
        long wait = limiter.tryAcquire("a", 3, window);
        assertThat(wait).isBetween(1L, 600L);

        clock.advance(Duration.ofMinutes(5));
        assertThat(limiter.tryAcquire("a", 3, window)).isPositive();      // the first tries are still inside the window
        clock.advance(Duration.ofMinutes(6));
        assertThat(limiter.tryAcquire("a", 3, window)).isZero();          // they have aged out
    }

    @Test
    void keysDoNotAffectEachOther() {
        RateLimiter limiter = new RateLimiter(new TestClock());
        assertThat(limiter.tryAcquire("one", 1, Duration.ofHours(1))).isZero();
        assertThat(limiter.tryAcquire("one", 1, Duration.ofHours(1))).isPositive();
        assertThat(limiter.tryAcquire("two", 1, Duration.ofHours(1))).isZero();
    }

    @Test
    void theFilterAnswers429OnlyForPostsToTheProtectedPaths() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RateLimitFilter filter = new RateLimitFilter(new RateLimiter(new TestClock()), mapper, 2, 2);

        // public forms: two allowed, the third refused
        assertThat(post(filter, "/api/v1/public/hunger-reports", "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(post(filter, "/api/v1/public/surplus-food-reports", "10.0.0.1").getStatus()).isEqualTo(200);
        MockHttpServletResponse refused = post(filter, "/api/v1/public/hunger-reports", "10.0.0.1");
        assertThat(refused.getStatus()).isEqualTo(429);
        assertThat(refused.getHeader("Retry-After")).isNotNull();
        assertThat(refused.getContentAsString()).contains("Too many attempts");
        // another address is unaffected
        assertThat(post(filter, "/api/v1/public/hunger-reports", "10.0.0.2").getStatus()).isEqualTo(200);

        // login has its own budget
        assertThat(post(filter, "/api/v1/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(post(filter, "/api/v1/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(post(filter, "/api/v1/auth/login", "10.0.0.1").getStatus()).isEqualTo(429);

        // reads and signed-in work are never limited
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/v1/public/localities");
            get.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse r = new MockHttpServletResponse();
            filter.doFilter(get, r, new MockFilterChain());
            assertThat(r.getStatus()).isEqualTo(200);
            assertThat(post(filter, "/api/v1/vendors", "10.0.0.1").getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletResponse post(RateLimitFilter filter, String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}

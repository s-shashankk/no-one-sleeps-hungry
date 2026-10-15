package com.mealup.security;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealup.common.api.ApiError;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Slows down abuse of the places anyone can reach: the public forms and the sign-in endpoint.
 * Runs before Spring Security so every attempt counts. The address is the one Tomcat reports; behind a
 * reverse proxy, the proxy must send X-Forwarded-For (see docs/DEPLOYMENT.md).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration PUBLIC_WINDOW = Duration.ofHours(1);
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(10);

    private final RateLimiter limiter;
    private final ObjectMapper mapper;
    private final int publicPerHour;
    private final int loginPerTenMinutes;

    public RateLimitFilter(RateLimiter limiter, ObjectMapper mapper,
                           @Value("${mealup.rate-limit.public-per-hour:20}") int publicPerHour,
                           @Value("${mealup.rate-limit.login-per-10-minutes:10}") int loginPerTenMinutes) {
        this.limiter = limiter;
        this.mapper = mapper;
        this.publicPerHour = publicPerHour;
        this.loginPerTenMinutes = loginPerTenMinutes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long retryAfter = 0;
        if ("POST".equals(request.getMethod())) {
            String path = request.getRequestURI();
            String ip = request.getRemoteAddr();
            if (path.startsWith("/api/v1/public/")) {
                retryAfter = limiter.tryAcquire("public:" + ip, publicPerHour, PUBLIC_WINDOW);
            } else if (path.equals("/api/v1/auth/login")) {
                retryAfter = limiter.tryAcquire("login:" + ip, loginPerTenMinutes, LOGIN_WINDOW);
            }
        }
        if (retryAfter > 0) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), ApiError.of(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts. Please wait a while and try again.", request.getRequestURI()));
            return;
        }
        chain.doFilter(request, response);
    }
}

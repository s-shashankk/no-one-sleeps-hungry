package com.mealup.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import com.mealup.auth.SessionUserRefreshFilter;
import com.mealup.users.UserRepository;

/**
 * Login is a server-side session in an HttpOnly cookie. State-changing requests need the CSRF header
 * (the frontend copies it from the XSRF-TOKEN cookie). Everything except the public forms and
 * sign-in needs a signed-in admin; services check this again through AccessPolicy.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SecurityErrorHandlers errors, UserRepository users,
                                    SecurityContextRepository contextRepository,
                                    @Value("${mealup.security.csp:}") String contentSecurityPolicy) throws Exception {
        // Plain handler: the token is created only when something asks for it (GET /auth/csrf).
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        http
            .csrf(c -> c
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfHandler))
            .headers(h -> {
                h.frameOptions(f -> f.deny());
                h.referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                h.permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=(), payment=()"));
                h.httpStrictTransportSecurity(t -> t.includeSubDomains(true).maxAgeInSeconds(31_536_000));
                // Only set in production (application-prod.yml): the dev Swagger page needs scripts of its own.
                if (!contentSecurityPolicy.isBlank()) {
                    h.contentSecurityPolicy(c -> c.policyDirectives(contentSecurityPolicy));
                }
            })
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .securityContext(c -> c.securityContextRepository(contextRepository))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .addFilterAfter(new SessionUserRefreshFilter(users), SecurityContextHolderFilter.class)
            .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/v1/health", "/error",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/logout").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()      // public forms; they only ever create pending records
                .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Login is done by AuthService, not by Spring's built-in providers. Declaring this bean stops Spring from
     * creating a random default user at startup.
     */
    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Not used.");
        };
    }
}

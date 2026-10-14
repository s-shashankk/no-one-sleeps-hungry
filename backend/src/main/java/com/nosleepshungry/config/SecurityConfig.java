package com.nosleepshungry.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nosleepshungry.security.JwtAuthFilter;
import com.nosleepshungry.security.JwtService;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/public/**", "/api/health", "/api/auth/login", "/error").permitAll()
                // Super Admin only
                .requestMatchers("/api/admin/volunteer-applications/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/admin/coordinators/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/admin/activity/**").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/admin/localities").hasRole("SUPER_ADMIN")
                // any signed-in admin (data is further limited to their localities in the code)
                .requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "COORDINATOR")
                .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}") String origins) {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> allowed = Arrays.stream(origins.split(",")).map(String::trim).toList();
        cfg.setAllowedOrigins(allowed);
        cfg.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }
}

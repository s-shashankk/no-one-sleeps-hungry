package com.mealup.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.mealup.support.ApiTestSupport;

/**
 * A review of every endpoint's permissions that cannot go out of date: it walks the real list of endpoints.
 * A new endpoint that forgets its access check shows up here as a failure.
 */
class EndpointPermissionsTest extends ApiTestSupport {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    private record Endpoint(HttpMethod method, String pattern) {
        String url() { return pattern.replaceAll("\\{[^}]+}", "1"); }
        @Override public String toString() { return method + " " + pattern; }
    }

    private List<Endpoint> endpoints() {
        List<Endpoint> out = new ArrayList<>();
        for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
            Set<String> patterns = info.getPathPatternsCondition().getPatternValues();
            Set<org.springframework.web.bind.annotation.RequestMethod> methods = info.getMethodsCondition().getMethods();
            for (String pattern : patterns) {
                if (!pattern.startsWith("/api/v1")) continue;
                for (var m : methods) out.add(new Endpoint(HttpMethod.valueOf(m.name()), pattern));
            }
        }
        return out;
    }

    /** Endpoints meant to be reachable without signing in. Everything else must answer 401. */
    private static boolean isPublic(Endpoint e) {
        String p = e.pattern();
        return p.equals("/api/v1/health") || p.startsWith("/api/v1/public/")
                || p.equals("/api/v1/auth/csrf") || p.equals("/api/v1/auth/login") || p.equals("/api/v1/auth/logout");
    }

    private MockHttpServletRequestBuilder build(Endpoint e) {
        MockHttpServletRequestBuilder b = request(e.method(), e.url());
        if (e.method() != HttpMethod.GET) {
            b.with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        return b;
    }

    @Test
    void theEndpointListIsReallyBeingChecked() {
        List<Endpoint> all = endpoints();
        assertThat(all.size()).isGreaterThan(20);
        assertThat(all.stream().filter(e -> !isPublic(e)).count()).isGreaterThan(15);
    }

    @Test
    void everyEndpointExceptThePublicOnesRequiresSignIn() throws Exception {
        List<String> leaks = new ArrayList<>();
        for (Endpoint e : endpoints()) {
            if (isPublic(e)) continue;
            int status = mvc.perform(build(e)).andReturn().getResponse().getStatus();
            if (status != 401) leaks.add(e + " answered " + status + " to an anonymous caller");
        }
        assertThat(leaks).isEmpty();
    }

    @Test
    void thePublicFormsAreTheOnlyWayInWithoutSigningInAndTheyOnlyAcceptPosts() {
        List<String> publicEndpoints = endpoints().stream().filter(e -> e.pattern().startsWith("/api/v1/public/"))
                .map(Endpoint::toString).sorted().toList();

        assertThat(publicEndpoints).containsExactly(
                "POST /api/v1/public/hunger-reports", "POST /api/v1/public/surplus-food-reports");
    }
}

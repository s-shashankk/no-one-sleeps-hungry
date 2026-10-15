package com.mealup.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.mealup.support.ApiTestSupport;
import com.mealup.users.User;

/** Login, logout, /me, CSRF, and "switched off means locked out immediately". */
class LoginAndSessionTest extends ApiTestSupport {

    @Test
    void loginReturnsTheProfileAndMeWorksWithTheSession() throws Exception {
        User admin = newAdmin();

        MockHttpSession session = login(admin.getEmail(), PASSWORD);

        getAs(session, "/api/v1/auth/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(admin.getEmail()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void meWithoutLoginIsUnauthorized() throws Exception {
        getAs(null, "/api/v1/auth/me").andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
        User admin = newAdmin();

        String wrongPassword = postJson("/api/v1/auth/login", Map.of("email", admin.getEmail(), "password", "nope-nope-nope"), null)
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknownEmail = postJson("/api/v1/auth/login", Map.of("email", "nobody@test.mealup", "password", "nope-nope-nope"), null)
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(json.readTree(wrongPassword).get("message"))
                .isEqualTo(json.readTree(unknownEmail).get("message"));
    }

    @Test
    void changingStateWithoutTheCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + FOUNDER_EMAIL + "\",\"password\":\"" + FOUNDER_PASSWORD + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfEndpointHandsOutAReadableCookie() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        MockHttpSession session = login(newAdmin().getEmail(), PASSWORD);

        postJson("/api/v1/auth/logout", Map.of(), session).andExpect(status().isNoContent());

        getAs(session, "/api/v1/auth/me").andExpect(status().isUnauthorized());
    }

    @Test
    void anAdminSwitchedOffIsLockedOutOnTheVeryNextRequest() throws Exception {
        User admin = newAdmin();
        MockHttpSession session = login(admin.getEmail(), PASSWORD);
        getAs(session, "/api/v1/auth/me").andExpect(status().isOk());

        admin.setActive(false);
        users.save(admin);

        getAs(session, "/api/v1/auth/me").andExpect(status().isUnauthorized());
        postJson("/api/v1/auth/login", Map.of("email", admin.getEmail(), "password", PASSWORD), null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginFailureMessageIsGeneric() throws Exception {
        postJson("/api/v1/auth/login", Map.of("email", "x@test.mealup", "password", "whatever-long-1"), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Incorrect email or password")));
    }
}

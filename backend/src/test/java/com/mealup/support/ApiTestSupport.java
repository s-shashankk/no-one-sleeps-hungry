package com.mealup.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealup.users.User;
import com.mealup.users.UserRepository;

/** Shared helpers for integration tests: sign in, call the API, build request bodies. */
public abstract class ApiTestSupport extends AbstractMySqlIntegrationTest {

    public static final String FOUNDER_EMAIL = "founder@test.mealup";
    public static final String FOUNDER_PASSWORD = "Test-Founder-Pass-1";
    public static final String PASSWORD = "A-good-test-password-1";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected UserRepository users;
    @Autowired protected PasswordEncoder encoder;

    protected String uniqueEmail() {
        return "u-" + UUID.randomUUID().toString().substring(0, 8) + "@test.mealup";
    }

    protected String uniquePhone() {
        return "+9190" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    protected User newAdmin() {
        return users.save(new User("Test Admin", uniquePhone(), uniqueEmail(), encoder.encode(PASSWORD)));
    }

    protected MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) postJson("/api/v1/auth/login", Map.of("email", email, "password", password), null)
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    protected MockHttpSession loginAsFounder() throws Exception {
        return login(FOUNDER_EMAIL, FOUNDER_PASSWORD);
    }

    protected ResultActions getAs(MockHttpSession session, String url) throws Exception {
        MockHttpServletRequestBuilder req = get(url);
        if (session != null) req.session(session);
        return mvc.perform(req);
    }

    protected ResultActions postJson(String url, Object body, MockHttpSession session) throws Exception {
        MockHttpServletRequestBuilder req = post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body));
        if (session != null) req.session(session);
        return mvc.perform(req);
    }

    protected ResultActions putJson(String url, Object body, MockHttpSession session) throws Exception {
        MockHttpServletRequestBuilder req = put(url).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body));
        if (session != null) req.session(session);
        return mvc.perform(req);
    }

    /** POSTs and returns the new record's id (expects 201). */
    protected long createdId(String url, Object body, MockHttpSession session) throws Exception {
        String response = postJson(url, body, session).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    // ---- request bodies ----------------------------------------------------------------------

    protected Map<String, Object> vendorBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Hotel-" + UUID.randomUUID().toString().substring(0, 8));
        body.put("contactPerson", "Asha Manager");
        body.put("phoneNumber", "+91 98765 43210");
        body.put("location", "12 Main Road, Central");
        return body;
    }

    protected Map<String, Object> coworkerBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Coworker-" + UUID.randomUUID().toString().substring(0, 8));
        body.put("phoneNumber", uniquePhone());
        body.put("location", "North side");
        return body;
    }

    protected Map<String, Object> needFoodBody(int people) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("location", "Under the flyover near the market");
        body.put("people", people);
        body.put("name", "Meera");
        body.put("phone", "+91 91234 56789");
        body.put("consent", true);
        return body;
    }

    protected Map<String, Object> surplusBody(int meals) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Grand Wedding Hall");
        body.put("phone", "+91 91234 56780");
        body.put("location", "Hall Road, Gate 2");
        body.put("meals", meals);
        body.put("details", "Rice, sambar, sweets. Ready till 10 pm");
        body.put("consent", true);
        return body;
    }

    /** Anyone submits the public "people need food" form; returns the id of the new request (found as admin). */
    protected long submitNeedFood(int people, MockHttpSession admin) throws Exception {
        postJson("/api/v1/public/hunger-reports", needFoodBody(people), null).andExpect(status().isCreated());
        return newestPendingId("NEED_FOOD", admin);
    }

    protected long submitSurplus(int meals, MockHttpSession admin) throws Exception {
        postJson("/api/v1/public/surplus-food-reports", surplusBody(meals), null).andExpect(status().isCreated());
        return newestPendingId("SURPLUS_FOOD", admin);
    }

    private long newestPendingId(String type, MockHttpSession admin) throws Exception {
        String list = getAs(admin, "/api/v1/requests?status=PENDING&type=" + type + "&sort=newest&size=1")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(list).get("content").get(0).get("id").asLong();
    }
}

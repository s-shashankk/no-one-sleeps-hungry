package com.mealup.coworkers;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.mealup.support.ApiTestSupport;

/** Add, view, edit and switch coworkers on and off. */
class CoworkerTest extends ApiTestSupport {

    @Test
    void addACoworker() throws Exception {
        MockHttpSession admin = loginAsFounder();
        Map<String, Object> body = coworkerBody();
        body.put("phoneNumber", uniquePhone());

        postJson("/api/v1/coworkers", body, admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(body.get("name")))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void nameNumberAndLocationAreRequired() throws Exception {
        MockHttpSession admin = loginAsFounder();
        Map<String, Object> noLocation = coworkerBody();
        noLocation.remove("location");
        Map<String, Object> badPhone = coworkerBody();
        badPhone.put("phoneNumber", "abc");

        postJson("/api/v1/coworkers", noLocation, admin).andExpect(status().isBadRequest());
        postJson("/api/v1/coworkers", badPhone, admin).andExpect(status().isBadRequest());
    }

    @Test
    void theSamePhoneNumberCannotBeAddedTwice() throws Exception {
        MockHttpSession admin = loginAsFounder();
        Map<String, Object> first = coworkerBody();
        createdId("/api/v1/coworkers", first, admin);
        Map<String, Object> again = coworkerBody();
        again.put("phoneNumber", first.get("phoneNumber"));

        postJson("/api/v1/coworkers", again, admin).andExpect(status().isConflict());
    }

    @Test
    void editChangesTheDetailsAndKeepsOwnNumber() throws Exception {
        MockHttpSession admin = loginAsFounder();
        Map<String, Object> body = coworkerBody();
        long id = createdId("/api/v1/coworkers", body, admin);
        body.put("location", "South side");

        putJson("/api/v1/coworkers/" + id, body, admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("South side"));
    }

    @Test
    void switchedOffCoworkersLeaveTheActiveList() throws Exception {
        MockHttpSession admin = loginAsFounder();
        Map<String, Object> body = coworkerBody();
        long id = createdId("/api/v1/coworkers", body, admin);
        String name = (String) body.get("name");

        body.put("active", false);
        putJson("/api/v1/coworkers/" + id, body, admin).andExpect(jsonPath("$.active").value(false));

        getAs(admin, "/api/v1/coworkers?active=true&q=" + name).andExpect(jsonPath("$.content[*].id", not(hasItem((int) id))));
        getAs(admin, "/api/v1/coworkers?active=false&q=" + name).andExpect(jsonPath("$.content[*].id", hasItem((int) id)));
    }

    @Test
    void coworkersAreNotVisibleWithoutSigningIn() throws Exception {
        getAs(null, "/api/v1/coworkers").andExpect(status().isUnauthorized());
    }
}

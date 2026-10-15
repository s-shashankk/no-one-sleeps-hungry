package com.mealup.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.mealup.support.ApiTestSupport;

/** The dashboard shows exactly four numbers. */
class DashboardTest extends ApiTestSupport {

    private JsonNode summary(MockHttpSession admin) throws Exception {
        return json.readTree(getAs(admin, "/api/v1/dashboard").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    void showsTheFourNumbersAndNothingElse() throws Exception {
        MockHttpSession admin = loginAsFounder();

        JsonNode s = summary(admin);

        assertThat(s.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("totalMealsDelivered", "activeVendors", "activeCoworkers", "pendingRequests");
    }

    @Test
    void activeVendorsAndCoworkersFollowTheyAreSwitchedOnAndOff() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long vendors = summary(admin).get("activeVendors").asLong();
        long coworkers = summary(admin).get("activeCoworkers").asLong();

        Map<String, Object> vendor = vendorBody();
        long vendorId = createdId("/api/v1/vendors", vendor, admin);
        Map<String, Object> coworker = coworkerBody();
        long coworkerId = createdId("/api/v1/coworkers", coworker, admin);
        assertThat(summary(admin).get("activeVendors").asLong()).isEqualTo(vendors + 1);
        assertThat(summary(admin).get("activeCoworkers").asLong()).isEqualTo(coworkers + 1);

        vendor.put("active", false);
        coworker.put("active", false);
        putJson("/api/v1/vendors/" + vendorId, vendor, admin).andExpect(status().isOk());
        putJson("/api/v1/coworkers/" + coworkerId, coworker, admin).andExpect(status().isOk());

        assertThat(summary(admin).get("activeVendors").asLong()).isEqualTo(vendors);
        assertThat(summary(admin).get("activeCoworkers").asLong()).isEqualTo(coworkers);
    }

    @Test
    void requiresSignIn() throws Exception {
        getAs(null, "/api/v1/dashboard").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }
}

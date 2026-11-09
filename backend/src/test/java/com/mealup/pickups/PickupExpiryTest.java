package com.mealup.pickups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.mealup.support.ApiTestSupport;

/** A booking nobody collects is closed automatically and frees the place. */
class PickupExpiryTest extends ApiTestSupport {

    @Autowired
    PickupExpiryJob job;

    @Test
    void staleBookingIsCancelledAutomatically() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long area = createdId("/api/v1/areas",
                Map.of("city", "Testville", "name", "Area-" + UUID.randomUUID().toString().substring(0, 8), "open", true), admin);
        Map<String, Object> hotelBody = vendorBody();
        hotelBody.put("areaId", area);
        hotelBody.put("dailyLimit", 500);
        long hotel = createdId("/api/v1/vendors", hotelBody, admin);

        MockHttpSession volunteer = registerVolunteer(uniqueEmail());
        Map<String, Object> pledge = new LinkedHashMap<>();
        pledge.put("vendorId", hotel);
        pledge.put("peopleCount", 10);
        pledge.put("deliverTo", "Near the bus stand");
        postJson("/api/v1/me/pickups", pledge, volunteer).andExpect(status().isCreated());

        // a cutoff in the future makes every open booking "old enough"
        int closed = job.expireCreatedBefore(LocalDateTime.now(ZoneOffset.UTC).plusHours(1));
        assertThat(closed).isGreaterThanOrEqualTo(1);

        JsonNode cancelled = json.readTree(getAs(admin, "/api/v1/pickups?status=CANCELLED&size=100")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("content");
        boolean found = false;
        for (JsonNode p : cancelled) found |= p.get("vendorId").asLong() == hotel;
        assertThat(found).isTrue();
    }
}

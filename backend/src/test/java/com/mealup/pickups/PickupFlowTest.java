package com.mealup.pickups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.mealup.support.ApiTestSupport;

/** Areas, hotels, volunteer pickups, plates served and join requests, end to end. */
class PickupFlowTest extends ApiTestSupport {

    private long newArea(MockHttpSession admin, boolean open) throws Exception {
        return createdId("/api/v1/areas",
                Map.of("city", "Testville", "name", "Area-" + UUID.randomUUID().toString().substring(0, 8), "open", open), admin);
    }

    private long hotelIn(long areaId, MockHttpSession admin) throws Exception {
        Map<String, Object> body = vendorBody();
        body.put("areaId", areaId);
        body.put("dailyLimit", 500);   // the tests book 30 plates at a time
        return createdId("/api/v1/vendors", body, admin);
    }

    private Map<String, Object> pledgeBody(long vendorId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vendorId", vendorId);
        body.put("peopleCount", 30);
        body.put("deliverTo", "Under the flyover");
        return body;
    }

    private long impactMeals() throws Exception {
        return json.readTree(getAs(null, "/api/v1/public/impact").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("mealsServed").asLong();
    }

    @Test
    void volunteerChoosesAreaAndHotelAndAdminRecordsPlatesServed() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long area = newArea(admin, true);
        long hotel = hotelIn(area, admin);

        // visitors see the hotel's name and address, never its phone number
        String hotels = getAs(null, "/api/v1/public/areas/" + area + "/hotels").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(hotels).get(0).get("id").asLong()).isEqualTo(hotel);
        assertThat(hotels).doesNotContain("phone");

        long before = impactMeals();
        String volunteerEmail = uniqueEmail();
        MockHttpSession volunteer = registerVolunteer(volunteerEmail);
        postJson("/api/v1/me/pickups", pledgeBody(hotel), volunteer).andExpect(status().isCreated())
                .andExpect(jsonPath("$.peopleCount").value(30));

        JsonNode pickup = json.readTree(getAs(admin, "/api/v1/pickups?status=PLEDGED&size=1").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("content").get(0);
        long id = pickup.get("id").asLong();
        assertThat(pickup.get("vendorId").asLong()).isEqualTo(hotel);
        assertThat(pickup.get("volunteerEmail").asText()).isEqualTo(volunteerEmail);
        assertThat(pickup.get("peopleCount").asInt()).isEqualTo(30);

        postJson("/api/v1/pickups/" + id + "/confirm", Map.of(), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        postJson("/api/v1/pickups/" + id + "/complete", Map.of("platesServed", 0), admin).andExpect(status().isBadRequest());
        postJson("/api/v1/pickups/" + id + "/complete", Map.of("platesServed", 40), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.platesServed").value(40));
        assertThat(impactMeals()).isEqualTo(before + 40);

        postJson("/api/v1/pickups/" + id + "/cancel", Map.of(), admin).andExpect(status().isConflict());
        postJson("/api/v1/pickups/" + id + "/reopen", Map.of(), admin).andExpect(status().isOk());
        assertThat(impactMeals()).isEqualTo(before);
    }

    @Test
    void aHotelInAComingSoonAreaCannotBeChosen() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long area = newArea(admin, false);
        long hotel = hotelIn(area, admin);

        assertThat(getAs(null, "/api/v1/public/areas/" + area + "/hotels").andReturn().getResponse().getContentAsString())
                .isEqualTo("[]");
        postJson("/api/v1/me/pickups", pledgeBody(hotel), registerVolunteer(uniqueEmail())).andExpect(status().isBadRequest());
    }

    @Test
    void aPledgeNeedsASignedInVolunteerAndTheNumberOfPeople() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long hotel = hotelIn(newArea(admin, true), admin);
        MockHttpSession volunteer = registerVolunteer(uniqueEmail());

        postJson("/api/v1/me/pickups", pledgeBody(hotel), null).andExpect(status().isUnauthorized());
        postJson("/api/v1/me/pickups", pledgeBody(hotel), admin).andExpect(status().isForbidden());

        Map<String, Object> noPeople = pledgeBody(hotel);
        noPeople.remove("peopleCount");
        postJson("/api/v1/me/pickups", noPeople, volunteer).andExpect(status().isBadRequest());
        Map<String, Object> zeroPeople = pledgeBody(hotel);
        zeroPeople.put("peopleCount", 0);
        postJson("/api/v1/me/pickups", zeroPeople, volunteer).andExpect(status().isBadRequest());
    }

    @Test
    void joinRequestsReachTheAdminAndCanBeMarkedContacted() throws Exception {
        MockHttpSession admin = loginAsFounder();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Kiran");
        body.put("phone", uniquePhone());
        body.put("email", "kiran@example.com");
        body.put("location", "Domlur");
        body.put("consent", true);
        postJson("/api/v1/public/join-requests", body, null).andExpect(status().isCreated());

        JsonNode j = json.readTree(getAs(admin, "/api/v1/join-requests?status=NEW&size=1").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("content").get(0);
        assertThat(j.get("name").asText()).isEqualTo("Kiran");

        postJson("/api/v1/join-requests/" + j.get("id").asLong() + "/contacted", Map.of(), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONTACTED"));
        getAs(null, "/api/v1/join-requests").andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateAreasAreRefused() throws Exception {
        MockHttpSession admin = loginAsFounder();
        Map<String, Object> area = Map.of("city", "Bengaluru", "name", "Indiranagar", "open", true);
        postJson("/api/v1/areas", area, admin).andExpect(status().isConflict());
    }

    // ---- hotel logos and the partners slider ------------------------------------------------------

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    private org.springframework.test.web.servlet.ResultActions upload(long vendorId, byte[] bytes, MockHttpSession admin) throws Exception {
        var file = new org.springframework.mock.web.MockMultipartFile("file", "logo.png", "image/png", bytes);
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .multipart("/api/v1/vendors/" + vendorId + "/logo").file(file).session(admin)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()));
    }

    @Test
    void aHotelLogoIsShownOnThePartnersListAndServedPublicly() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long hotel = hotelIn(newArea(admin, true), admin);

        upload(hotel, PNG, admin).andExpect(status().isOk()).andExpect(jsonPath("$.logoVersion").isNumber());

        String partners = getAs(null, "/api/v1/public/partners").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode mine = null;
        for (JsonNode p : json.readTree(partners)) if (p.get("id").asLong() == hotel) mine = p;
        assertThat(mine).isNotNull();
        assertThat(mine.get("logoVersion").isNumber()).isTrue();
        assertThat(partners).doesNotContain("phone");

        getAs(null, "/api/v1/public/hotels/" + hotel + "/logo").andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/png"));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/vendors/" + hotel + "/logo").session(admin)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());
        getAs(null, "/api/v1/public/hotels/" + hotel + "/logo").andExpect(status().isNotFound());
    }

    @Test
    void onlyRealPngJpegAndWebpImagesAreAccepted() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long hotel = hotelIn(newArea(admin, true), admin);

        upload(hotel, "<svg onload=alert(1)></svg>".getBytes(), admin).andExpect(status().isBadRequest());
        upload(hotel, new byte[0], admin).andExpect(status().isBadRequest());
        upload(hotel, new byte[600 * 1024], admin).andExpect(status().is4xxClientError());
        getAs(null, "/api/v1/public/hotels/" + hotel + "/logo").andExpect(status().isNotFound());
    }
}
